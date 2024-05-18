package com.example.util;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.model.BasicMessageModel;
import com.example.model.DeviceModel;
import com.example.model.EmployeeModel;
import com.example.model.HeartBeatModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class UtilMethods {
    public static final String broker = "tcp://192.168.1.4:1883"; // MQTT broker address
    public static final String LOG_PUSH_TOPIC = "mqtt/face/%s/Rec";
    public static final String ACK_TOPIC = "mqtt/face/%s";
    public static final String HEARTBEAT_PUSH_TOPIC = "mqtt/face/heartbeat";
    JSONObject jsonObject;
    String resourceName = "log_sample.json";
    Random random = new Random();
    List<DeviceModel> deviceModels;
    List<EmployeeModel> employeeModels;
    int deviceIdStart = 10000;
    String deviceNamePrefix = "Device-Mac";

    public UtilMethods(int numberOfDevices, int numberOfEmployees,
            int deviceIdStart, String deviceNamePrefix) {
        this.deviceIdStart = deviceIdStart;
        this.deviceNamePrefix = deviceNamePrefix;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (inputStream != null) {

            try {
                jsonObject = new JSONObject(new String(inputStream.readAllBytes()));
            } catch (JSONException e) {

                e.printStackTrace();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
        generateDeviceModels(numberOfDevices);
        generateEmployeeModels(numberOfEmployees);

    }

    public String createJson(BasicMessageModel messageModel) {
        if (jsonObject == null) {
            System.err.println("Resource not found: " + resourceName);
            return "";
        }
        JSONObject infoObject = jsonObject.getJSONObject("info");

        // Modify the specified fields
        infoObject.put("customId", messageModel.getCustomId());
        infoObject.put("RecordID", messageModel.getRecordId());
        infoObject.put("facesluiceId", messageModel.getDeviceId());
        infoObject.put("facesluiceName", messageModel.getDeviceName());
        infoObject.put("persionName", messageModel.getPersonName());

        return jsonObject.toString(); // Changed to use toString() method

    }

    public String createHeartBeatJson(HeartBeatModel heartBeatModel) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operator", "HeartBeat");
        JSONObject infoObject = new JSONObject();
        // Modify the specified fields
        infoObject.put("facesluiceId", heartBeatModel.getDeviceId());
        infoObject.put("time", heartBeatModel.getTimeStamp());
        jsonObject.put("info", infoObject);
        return jsonObject.toString(); // Changed to use toString() method

    }

    public void generateDeviceModels(int numberOfDevices) {
        List<DeviceModel> deviceModelsDummy = new ArrayList<>(numberOfDevices);
        for (int i = 0; i < numberOfDevices; i++) {

            deviceModelsDummy.add(new DeviceModel("" + (deviceIdStart + i), deviceNamePrefix + i));
        }
        this.deviceModels = deviceModelsDummy;
    }

    public void generateEmployeeModels(int numberOfEmployees) {
        List<EmployeeModel> employeeModelsDummy = new ArrayList<>(numberOfEmployees);
        for (int i = 0; i < numberOfEmployees; i++) {
            employeeModelsDummy.add(new EmployeeModel("Person-" + 1, "E00" + i));
        }
        this.employeeModels = employeeModelsDummy;
    }

    public DeviceModel getRandomDevice() {
        int randomIndex = random.nextInt(deviceModels.size());
        return deviceModels.get(randomIndex);
    }

    public EmployeeModel getRandomEmployee() {
        int randomIndex = random.nextInt(employeeModels.size());
        return employeeModels.get(randomIndex);
    }

    public BasicMessageModel createMessageModel(int recordId) {
        return createMessageModel(getRandomDevice(), recordId);
    }

    public BasicMessageModel createMessageModel(DeviceModel deviceModel, int recordId) {

        EmployeeModel employeeModel = getRandomEmployee();
        return new BasicMessageModel(
                employeeModel.getCustomId(),
                employeeModel.getPersonName(),
                "" + recordId,
                deviceModel.getDeviceId(),
                deviceModel.getDeviceName());
    }

    public HeartBeatModel createHeartBeatModel(DeviceModel deviceModel) {

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = dateFormat.format(currentDate);

        HeartBeatModel heartBeatModel = new HeartBeatModel(
                deviceModel.getDeviceId(), formattedDate);
        return heartBeatModel;
    }

    public int getNumberOfDevices() {
        return deviceModels.size();
    }

    public List<DeviceModel> getDeviceModels() {
        return deviceModels;
    }

    public Boolean getRandomBoolean() {
        int rand = random.nextInt(2);
        if (rand == 1) {
            return true;
        } else {
            return false;
        }
        // return rand ==1;
    }

}
