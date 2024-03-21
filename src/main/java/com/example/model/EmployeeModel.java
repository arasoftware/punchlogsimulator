package com.example.model;

public class EmployeeModel {
    private String personName;
    private String customId;

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public EmployeeModel(String personName, String customId) {
        this.personName = personName;
        this.customId = customId;
    }

    @Override
    public String toString() {
        return "[personName=" + personName + ", customId=" + customId + "]";
    }

}
