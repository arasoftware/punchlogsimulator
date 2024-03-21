package com.example.model;

public class BasicMessageModel {
    private String recordId;
    private String deviceId;
    private String deviceName;
    private String customId;
    private String personName;

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public BasicMessageModel(String customId, String personName, String recordId, String deviceId, String deviceName) {
        this.customId = customId;
        this.personName = personName;
        this.recordId = recordId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    @Override
    public String toString() {
        return " {recordId=" + recordId + ", deviceId=" + deviceId + ", deviceName=" + deviceName
                + ", customId=" + customId + ", personName=" + personName + "}";
    }

}
