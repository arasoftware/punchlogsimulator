package com.example.functionality;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.example.model.BasicMessageModel;
import com.example.model.DeviceModel;
import com.example.util.UtilMethods;

public class PushInMemoryLogSimulator {

    private int numberOfLogs;
    private final UtilMethods utilMethods;
    private ExecutorService deviceThreads;

    public PushInMemoryLogSimulator(int numberOfDevices, int numberOfEmployees,
            int numberOfLogs, int deviceIdStart, String deviceNamePrefix) {
        this.numberOfLogs = numberOfLogs;
        utilMethods = new UtilMethods(numberOfDevices, numberOfEmployees, deviceIdStart, deviceNamePrefix);
    }

    private List<InMemoryDeviceSimulator> prepareDevices() throws MqttException {

        List<InMemoryDeviceSimulator> deviceSimulators = new ArrayList<>(utilMethods.getNumberOfDevices());
        int logCountPerDevice = numberOfLogs / utilMethods.getNumberOfDevices();

        for (DeviceModel aDeviceModel : utilMethods.getDeviceModels()) {
            List<BasicMessageModel> messageModels = createSampleMessageModels(logCountPerDevice, aDeviceModel);
            deviceSimulators.add(new InMemoryDeviceSimulator(messageModels, utilMethods, aDeviceModel));
        }
        return deviceSimulators;
    }

    public void runAll() throws MqttException {
        List<InMemoryDeviceSimulator> deviceSimulators = prepareDevices();

        deviceThreads = Executors.newFixedThreadPool(utilMethods.getNumberOfDevices());
        deviceSimulators.stream()
                .forEach(task -> deviceThreads.submit(task));

    }

    private List<BasicMessageModel> createSampleMessageModels(int numberOfMessage, DeviceModel forDevice) {
        ArrayList<BasicMessageModel> messageModels = new ArrayList<>(numberOfMessage);
        for (int i = 0; i < numberOfMessage; i++) {
            messageModels.add(utilMethods.createMessageModel(forDevice, i + 1));
        }
        return messageModels;
    }
}
