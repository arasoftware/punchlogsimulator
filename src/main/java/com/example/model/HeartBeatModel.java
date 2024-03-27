package com.example.model;

public class HeartBeatModel {

    private String deviceId;
    private String timeStamp;


    public HeartBeatModel(String deviceId,String timeStamp) {
      this.deviceId=deviceId;
      this.timeStamp=timeStamp;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
    
}
