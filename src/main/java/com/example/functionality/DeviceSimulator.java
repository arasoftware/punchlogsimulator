package com.example.functionality;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import com.example.model.BasicMessageModel;
import com.example.model.DeviceModel;
import com.example.util.UtilMethods;

public class DeviceSimulator implements Runnable {
    private static final Logger logger = LogManager.getLogger(DeviceSimulator.class);
    private MqttClient mqttClient;
    UtilMethods utilMethods;
    AtomicBoolean shouldRun;
    DeviceModel deviceModel;

    public DeviceSimulator(MqttClient mqttClient, UtilMethods utilMethods, AtomicBoolean shouldRun,
            DeviceModel deviceModel) {
        this.mqttClient = mqttClient;
        this.utilMethods = utilMethods;
        this.shouldRun = shouldRun;
        this.deviceModel = deviceModel;
    }

    @Override
    public void run() {
        int recordId = 1;
        while (shouldRun.get()) {
            publishAndWait(recordId);
            recordId++;
        }
        logger.info("Stopped {}", deviceModel);
    }

    private void publishAndWait(int recordId) {
        try {
            BasicMessageModel basicMessageModel = utilMethods.createMessageModel(deviceModel, recordId);
            String message = utilMethods.createJson(basicMessageModel);
            mqttClient.publish(UtilMethods.LOG_PUSH_TOPIC, message.getBytes(), 1, false);
            logger.info("Published message " + basicMessageModel);
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

}
