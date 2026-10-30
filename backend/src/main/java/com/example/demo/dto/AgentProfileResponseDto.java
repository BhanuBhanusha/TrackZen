package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentProfileResponseDto {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String department;
    private String specialization;
    private int currentLoad;
    private int maxCapacity;

    @JsonProperty("isAvailable")
    private boolean isAvailable;

    private double loadPercentage;

    public AgentProfileResponseDto() {
    }

    public AgentProfileResponseDto(Long id, Long userId, String fullName, String email, String department, String specialization, int currentLoad, int maxCapacity, boolean isAvailable, double loadPercentage) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.department = department;
        this.specialization = specialization;
        this.currentLoad = currentLoad;
        this.maxCapacity = maxCapacity;
        this.isAvailable = isAvailable;
        this.loadPercentage = loadPercentage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public double getLoadPercentage() {
        return loadPercentage;
    }

    public void setLoadPercentage(double loadPercentage) {
        this.loadPercentage = loadPercentage;
    }
}
