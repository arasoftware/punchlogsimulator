package com.example.functionality;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.example.util.UtilMethods;

public class InMemoryDeviceSimulator implements Runnable, MqttCallback {
    private static final Logger logger = LogManager.getLogger(InMemoryDeviceSimulator.class);
    private MqttClient mqttClient;
    DeviceModel deviceModel;
    List<BasicMessageModel> messageModels;
    UtilMethods utilMethods;

    private AtomicBoolean hasAckReceived;

    public InMemoryDeviceSimulator(List<BasicMessageModel> messageModels, UtilMethods utilMethods,
            DeviceModel deviceModel)
            throws MqttException {
        this.deviceModel = deviceModel;
        this.utilMethods = utilMethods;
        hasAckReceived = new AtomicBoolean();
        this.messageModels = messageModels;
        mqttClient = new MqttClient(UtilMethods.broker, deviceModel.getDeviceId());
        mqttClient.setCallback(this); // Set the callback before connecting
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(false);
        // Set keep-alive interval to 300 seconds (5 minutes)
        connOpts.setKeepAliveInterval(300);
        // connOpts.setUserName("admin");
        // connOpts.setPassword("secret".toCharArray());
        mqttClient.connect(connOpts);
        String topicName = String.format(UtilMethods.ACK_TOPIC, deviceModel.getDeviceId());
        mqttClient.subscribe(topicName);
    }

    private void closeMqttConnection() {
        try {
            mqttClient.disconnect();
            mqttClient.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        publishLog();
    }

    private void publishLog() {

        String topicName = String.format(UtilMethods.LOG_PUSH_TOPIC, deviceModel.getDeviceId());
        while (messageModels.size() > 0) {
            try {
                int lastIndex = messageModels.size() - 1;
                if (hasAckReceived.get()) {
                    messageModels.remove(lastIndex);
                }
                lastIndex = messageModels.size() - 1;
                BasicMessageModel basicMessageModel = messageModels.get(lastIndex);
                String message = utilMethods.createJson(basicMessageModel);

                mqttClient.publish(topicName, message.getBytes(), 1, false);
                hasAckReceived.set(false);
                Thread.sleep(10000);
                logger.info("Published Record {} of {} device", lastIndex, deviceModel.getDeviceName());

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

        logger.info("Log Stopped {}", deviceModel);

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
            hasAckReceived.set(true);
            logger.info("ACK Arrived recordId is  {} for device {}", recordId, deviceId);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.info("Delivery completed for {}", deviceModel);
    }

}
