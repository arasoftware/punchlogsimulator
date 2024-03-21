package com.example.functionality;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;

import com.example.model.DeviceModel;
import com.example.util.UtilMethods;

public class DeviceThreadManager {
    private static final Logger logger = LogManager.getLogger(DeviceThreadManager.class);
    UtilMethods utilMethods;
    ExecutorService deviceThreads;
    private AtomicBoolean shouldRun;
    UtilMethods utilityMethods;
    int numberOfDevices;
    Map<String, DeviceSimulator> deviceMap;
    AtomicInteger logCounter;

    public DeviceThreadManager(int numberOfDevices) throws MqttException {
        this.numberOfDevices = numberOfDevices;
        this.utilMethods = new UtilMethods(numberOfDevices, 5);
        this.logCounter = new AtomicInteger();
        // Enable logging for MQTT
        shouldRun = new AtomicBoolean(true);
        deviceMap = new ConcurrentHashMap<>(numberOfDevices);

    }

    public void publishWithRetry() {
        try {
            List<DeviceModel> deviceModels = utilMethods.getDeviceModels();
            deviceThreads = Executors.newFixedThreadPool(numberOfDevices);
            for (DeviceModel deviceModel : deviceModels) {
                DeviceSimulator deviceSimulator = new DeviceSimulator(logCounter, utilMethods, shouldRun, deviceModel);
                deviceThreads.submit(deviceSimulator);
                deviceMap.put(deviceModel.getDeviceId(), deviceSimulator);
            }
        } catch (MqttException mqttException) {
            logger.error(mqttException.getMessage());
        }
    }

    public void stop() {

        try {
            shouldRun.set(false);
            deviceThreads.shutdown();
            deviceThreads.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("All Devices are stopped");

    }

}
