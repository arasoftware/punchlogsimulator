package com.example.functionality;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
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
import com.example.util.MongoDBConnection;
import com.example.util.UtilMethods;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class DeviceSimulator implements Runnable, MqttCallback {
    private static final Logger logger = LogManager.getLogger(DeviceSimulator.class);
    private MqttClient mqttClient;
    UtilMethods utilMethods;
    AtomicBoolean shouldRun;
    DeviceModel deviceModel;
    AtomicInteger logCounter;
    AtomicInteger duplicateLogCounter;
    AtomicInteger heartBeatCounter = new AtomicInteger(0);
    MessageSimulationQueue simQueue;
    private MongoDBConnection connection;

    public DeviceSimulator(AtomicInteger logCounter, AtomicInteger dupInteger, UtilMethods utilMethods,
            AtomicBoolean shouldRun,
            DeviceModel deviceModel) throws MqttException {
        simQueue = new MessageSimulationQueue();
        this.utilMethods = utilMethods;
        this.shouldRun = shouldRun;
        this.deviceModel = deviceModel;
        this.logCounter = logCounter;
        this.duplicateLogCounter = dupInteger;
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
        // connection = new MongoDBConnection();
    }

    public void run() {
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Wait for both threads to finish
        try {

            // Future<?> makingLogThread = executorService.submit(() -> makingLog());
            Future<?> logThread = executorService.submit(() -> publishLog());
            Future<?> heartBeatThread = executorService.submit(() -> publishHeartBeat());
            // Future<?> mLogHitThread = executorService.submit(()-> logHit());

            // makingLogThread.get();
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

    private void logHit() {

        while (shouldRun.get()) {
            H2DatabaseManager.getLastData(deviceModel.getDeviceId());

        }

    }

    private void publishHeartBeat() {

        while (shouldRun.get()) {

            try {
                HeartBeatModel heartBeatModel = utilMethods.createHeartBeatModel(deviceModel);
                String message = utilMethods.createHeartBeatJson(heartBeatModel);
                String topicName = String.format(UtilMethods.HEARTBEAT_PUSH_TOPIC);
                mqttClient.publish(topicName, message.getBytes(), 1, false);
                // logger.info("Published heartbeat for " + heartBeatModel.getDeviceId());
                // logger.info("Published heartbeat Count: {}",
                // heartBeatCounter.incrementAndGet());
                Thread.sleep(15000);
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

        logger.info("Heartbeat Stopped {}", deviceModel);

    }

    private void closeMqttConnection() {
        try {
            mqttClient.disconnect();
            mqttClient.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void insertBasicMessage(BasicMessageModel message) {

        String deviceId = message.getDeviceId();
        String recordId = message.getRecordId();

        // Check if a document with the same deviceId and recordId already exists
        Document existingDoc = connection.getCollection()
                .find(Filters.and(
                        Filters.eq("deviceId", deviceId),
                        Filters.eq("recordId", recordId)))
                .first();

        if (existingDoc == null) {
            Document doc = new Document("recordId", message.getRecordId())
                    .append("deviceId", message.getDeviceId())
                    .append("deviceName", message.getDeviceName())
                    .append("customId", message.getCustomId())
                    .append("personName", message.getPersonName());
            connection.getCollection().insertOne(doc);
        }
    }

    public BasicMessageModel getLatestEntryByDeviceId(String deviceId) {
        Document latestDoc = connection.getCollection()
                .find(Filters.eq("deviceId", deviceId))
                .sort(Sorts.descending("_id"))
                .first();

        if (latestDoc != null) {
            BasicMessageModel latestMessage = new BasicMessageModel();
            latestMessage.setRecordId(latestDoc.getString("recordId"));
            latestMessage.setDeviceId(latestDoc.getString("deviceId"));
            latestMessage.setDeviceName(latestDoc.getString("deviceName"));
            latestMessage.setCustomId(latestDoc.getString("customId"));
            latestMessage.setPersonName(latestDoc.getString("personName"));
            return latestMessage;
        }

        return null; // No matching document found
    }

    private void makingLog() {

        int recordId = 1;
        BasicMessageModel basicMessageModel = utilMethods.createMessageModel(deviceModel, recordId);
        // String message = utilMethods.createJson(basicMessageModel);
        // TODO: Add message to List, and index of the List to HashTable
        // simQueue.addMessage(recordId + "", message);
        insertBasicMessage(basicMessageModel);
        // H2DatabaseManager.addData(deviceModel.getDeviceId(),
        // String.valueOf(recordId), message);
        while (shouldRun.get()) {
            try {
                Thread.sleep(2000);
                boolean shouldCreateNewRecord = utilMethods.getRandomBoolean();
                if (shouldCreateNewRecord) {

                    recordId++;
                    BasicMessageModel bMessageModel = utilMethods.createMessageModel(deviceModel, recordId);
                    insertBasicMessage(bMessageModel);
                    // message = utilMethods.createJson(basicMessageModel);
                    // simQueue.addMessage(recordId + "", message);
                    // H2DatabaseManager.addData(deviceModel.getDeviceId(),
                    // String.valueOf(recordId), message);
                    // TODO: Add Record message to HashMap and List
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        logger.info("Making Log Stopped {}", deviceModel);

    }

    private void publishLog() {

        int recordId = 1;
        String duplicateMsg = null;
        // TODO: Add message to List, and index of the List to HashTable
        // simQueue.addMessage(recordId + "", message);

        String topicName = String.format(UtilMethods.LOG_PUSH_TOPIC, deviceModel.getDeviceId());
        while (shouldRun.get()) {
            try {
                // TODO: get last message from the List and publish
                // Optional<String> optionalMessage = simQueue.getLatestMessage();
                // BasicMessageModel basicMessageModel =
                // utilMethods.createMessageModel(deviceModel, recordId);
                // String message = utilMethods.createJson(basicMessageModel);
                boolean shouldCreateNewRecord = utilMethods.getRandomBoolean();
                if (shouldCreateNewRecord) {

                    BasicMessageModel basicMessageModel = utilMethods.createMessageModel(deviceModel, recordId);
                    String message = utilMethods.createJson(basicMessageModel);
                    Thread.sleep(1000);
                    mqttClient.publish(topicName, message.getBytes(), 1, false);
                    // logger.info("Published message " + basicMessageModel);
                    logger.info("Published Record Count: {}", logCounter.incrementAndGet());
                    recordId++;
                    duplicateMsg = message;
                    /// simQueue.addMessage(recordId + "", message);
                    // TODO: Add Record message to HashMap and List

                } else {
                    if (duplicateMsg != null) {
                        mqttClient.publish(topicName, duplicateMsg.getBytes(), 1, false);
                        logger.info("Published Duplicate Record Count: {}", duplicateLogCounter.incrementAndGet());
                    }
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

        logger.info("Log Stopped {}", deviceModel);

    }

    private void publishLog2() {

        while (shouldRun.get()) {
            try {
                BasicMessageModel basicMessageModel = getLatestEntryByDeviceId(deviceModel.getDeviceId());
                BasicMessageModel prevModel = null;
                String topicName = String.format(UtilMethods.LOG_PUSH_TOPIC, deviceModel.getDeviceId());
                boolean shouldCreateNewRecord = utilMethods.getRandomBoolean();

                if (basicMessageModel == null) {
                    logger.info("All Logs Processed.......................................................");
                    break;
                }
                if (shouldCreateNewRecord) {
                    // Thread.sleep(1000);
                    // TODO: get last message from the List and publish
                    String message = utilMethods.createJson(basicMessageModel);
                    Thread.sleep(100);
                    mqttClient.publish(topicName, message.getBytes(), 1, false);

                    logger.info("Published Record Count: {}", logCounter.incrementAndGet());
                    logger.info("Published message ");
                    prevModel = basicMessageModel;
                } else {
                    if (prevModel != null) {
                        String msg = utilMethods.createJson(prevModel);
                        mqttClient.publish(topicName, msg.getBytes(), 1, false);
                        logger.info("Published Duplicate Record Count: {}", duplicateLogCounter.incrementAndGet());
                    }

                }
            }

            catch (InterruptedException e) {
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
            // TODO: find index from HashMap and remove from List
            // H2DatabaseManager.deleteData(deviceId, recordId);
            // simQueue.removeMessage(recordId);
            // deleteByDeviceIdAndRecordId(deviceId, recordId);

            logger.info("message Arrived recordId is  {} for device {}", recordId, deviceId);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.info("Delivery completed for {}", deviceModel);
    }

    public void deleteByDeviceIdAndRecordId(String deviceId, String recordId) {
        connection.getCollection().deleteOne(Filters.and(
                Filters.eq("deviceId", deviceId),
                Filters.eq("recordId", recordId)));
        System.out.println("Deleted document with deviceId: " + deviceId + " and recordId: " + recordId);
    }

}
