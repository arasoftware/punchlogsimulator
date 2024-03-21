package com.example.functionality;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import com.example.model.BasicMessageModel;
import com.example.model.DeviceModel;
import com.example.util.UtilMethods;

public class DeviceSimulator implements Runnable, MqttCallback {
    private static final Logger logger = LogManager.getLogger(DeviceSimulator.class);
    private MqttClient mqttClient;
    UtilMethods utilMethods;
    AtomicBoolean shouldRun;
    DeviceModel deviceModel;
    AtomicInteger logCounter;

    public DeviceSimulator(AtomicInteger logCounter, UtilMethods utilMethods, AtomicBoolean shouldRun,
            DeviceModel deviceModel) throws MqttException {

        this.utilMethods = utilMethods;
        this.shouldRun = shouldRun;
        this.deviceModel = deviceModel;
        this.logCounter = logCounter;
        mqttClient = new MqttClient(UtilMethods.broker, deviceModel.getDeviceId());
        mqttClient.setCallback(this); // Set the callback before connecting
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        // Set keep-alive interval to 300 seconds (5 minutes)
        connOpts.setKeepAliveInterval(300);
        mqttClient.connect(connOpts);
    }

    @Override
    public void run() {
        int recordId = 1;
        while (shouldRun.get()) {
            publishAndWait(recordId);
            recordId++;
        }
        closeMqttConnection();
        logger.info("Stopped {}", deviceModel);
    }

    private void closeMqttConnection() {
        try {
            mqttClient.disconnect();
            mqttClient.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publishAndWait(int recordId) {
        try {
            BasicMessageModel basicMessageModel = utilMethods.createMessageModel(deviceModel, recordId);
            String message = utilMethods.createJson(basicMessageModel);
            String topicName = String.format(UtilMethods.LOG_PUSH_TOPIC, deviceModel.getDeviceId());
            mqttClient.publish(topicName, message.getBytes(), 1, false);
            logger.info("Published message " + basicMessageModel);
            logger.info("Published Record Count: {}", logCounter.incrementAndGet());
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } catch (MqttPersistenceException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } catch (MqttException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.info("Connection Lost.... {}", deviceModel);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        logger.info("message Arrived {} for topic {}", deviceModel, topic);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.info("Delivery completed for {}", deviceModel);
    }

}
