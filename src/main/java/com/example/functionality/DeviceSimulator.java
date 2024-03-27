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
import org.json.JSONObject;

import com.example.model.BasicMessageModel;
import com.example.model.DeviceModel;
import com.example.model.HeartBeatModel;
import com.example.util.UtilMethods;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class DeviceSimulator implements Runnable, MqttCallback {
    private static final Logger logger = LogManager.getLogger(DeviceSimulator.class);
    private MqttClient mqttClient;
    UtilMethods utilMethods;
    AtomicBoolean shouldRun;
    DeviceModel deviceModel;
    AtomicInteger logCounter;
    AtomicInteger heartBeatCounter = new AtomicInteger(0);
    AtomicBoolean nextLog=new AtomicBoolean(true);

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
        String topicName = String.format(UtilMethods. ACK_TOPIC, deviceModel.getDeviceId());
        mqttClient.subscribe(topicName);
    }

    @Override
    public void run() {
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        Future<?> logThread = executorService.submit(() -> publishLog());
        Future<?> heartBeatThread = executorService.submit(() -> publishHeartBeat());
        // Wait for both threads to finish
        try {
            logThread.get();
            heartBeatThread.get();
        } catch (InterruptedException | ExecutionException e) {
            // Handle exceptions
            e.printStackTrace();
        }
        executorService.shutdown();
        closeMqttConnection();
        logger.info("Stopped {}", deviceModel);

    }

    private void publishHeartBeat() {

        while (shouldRun.get()) {

            try {
                HeartBeatModel heartBeatModel = utilMethods.createHeartBeatModel(deviceModel);
                String message = utilMethods.createHeartBeatJson(heartBeatModel);
                String topicName = String.format(UtilMethods.HEARTBEAT_PUSH_TOPIC);
                mqttClient.publish(topicName, message.getBytes(), 1, false);
                logger.info("Published heartbeat  for " + heartBeatModel.getDeviceId());
                logger.info("Published heartbeat Count: {}", heartBeatCounter.incrementAndGet());
                Thread.sleep(10000);
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

        logger.info("Heartbeat Stopped {}",deviceModel);

    }

    private void closeMqttConnection() {
        try {
            mqttClient.disconnect();
            mqttClient.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publishLog() {

        int recordId = 1;
        BasicMessageModel basicMessageModel = utilMethods.createMessageModel(deviceModel, recordId);
        String message = utilMethods.createJson(basicMessageModel);

        while (shouldRun.get()) {
            try {
                String topicName = String.format(UtilMethods.LOG_PUSH_TOPIC, deviceModel.getDeviceId());
                mqttClient.publish(topicName, message.getBytes(), 1, false);
                nextLog.set(false);
                logger.info("Published message " + basicMessageModel);
             
                Thread.sleep(1000);
                if(nextLog.get()){
                    logger.info("Published Record Count: {}", logCounter.incrementAndGet());
                    recordId++;
                    basicMessageModel = utilMethods.createMessageModel(deviceModel, recordId);
                    message = utilMethods.createJson(basicMessageModel);
                    
                }
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

        logger.info("Log Stopped {}",deviceModel);

    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.info("Connection Lost.... {}", deviceModel);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        byte[] payload = message.getPayload();
        String payloadString = new String(payload);
        JSONObject jsonPayload = new JSONObject(payloadString);
        String operator = jsonPayload.getString("operator");
        JSONObject infoObject = jsonPayload.getJSONObject("info");

        String[] parts = topic.split("/");
        String deviceId = parts[parts.length - 1];

        if (operator.compareTo("PushAck") == 0) {
            String recordId = infoObject.getString("SnapOrRecordID");
            nextLog.set(true);
            logger.info("message Arrived recordId is  {} for device {}", recordId, deviceId);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.info("Delivery completed for {}", deviceModel);
    }

}
