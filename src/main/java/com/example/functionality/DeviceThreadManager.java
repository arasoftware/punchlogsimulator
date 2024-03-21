package com.example.functionality;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;

import com.example.model.DeviceModel;
import com.example.util.UtilMethods;

public class DeviceThreadManager implements MqttCallback {
    private static final Logger logger = LogManager.getLogger(DeviceThreadManager.class);
    private static final String broker = "tcp://164.52.203.123:1883"; // MQTT broker address
    private static final String clientId = "111111"; // Client ID
    MqttClient mqttClient = null;
    UtilMethods utilMethods;
    ExecutorService deviceThreads;
    private AtomicBoolean shouldRun;
    UtilMethods utilityMethods;
    int numberOfDevices;
    Map<String, DeviceSimulator> deviceMap;

    public DeviceThreadManager(int numberOfDevices) throws MqttException {
        this.numberOfDevices = numberOfDevices;
        this.utilMethods = new UtilMethods(numberOfDevices, 5);
        mqttClient = new MqttClient(broker, clientId);
        mqttClient.setCallback(this); // Set the callback before connecting
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        // Set keep-alive interval to 300 seconds (5 minutes)
        connOpts.setKeepAliveInterval(300);
        mqttClient.connect(connOpts);
        // Enable logging for MQTT
        shouldRun = new AtomicBoolean(true);
        deviceMap = new ConcurrentHashMap<>(numberOfDevices);

    }

    public void publishWithRetry() {

        List<DeviceModel> deviceModels = utilMethods.getDeviceModels();
        deviceThreads = Executors.newFixedThreadPool(numberOfDevices);
        for (DeviceModel deviceModel : deviceModels) {
            DeviceSimulator deviceSimulator = new DeviceSimulator(mqttClient, utilMethods, shouldRun, deviceModel);
            deviceThreads.submit(deviceSimulator);
            deviceMap.put(deviceModel.getDeviceId(), deviceSimulator);
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        logger.info("Connection lost!");
    }

    @Override
    public void messageArrived(String topicName, MqttMessage mqttMessage) throws Exception {
        logger.info("Message arrived on topic: " + topicName);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        logger.info(iMqttDeliveryToken.getMessageId() + " delivery completed...");
    }

    public void stop() {

        try {
            shouldRun.set(false);
            deviceThreads.shutdown();
            deviceThreads.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            mqttClient.disconnect();
            mqttClient.close();

        } catch (InterruptedException | MqttException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.info("All Devices are stopped");

    }

}
