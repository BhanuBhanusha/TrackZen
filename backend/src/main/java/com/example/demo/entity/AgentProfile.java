package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "agent_profiles")
public class AgentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private AppUser user;

    private String department = "General Support";

    private String specialization;

    @Column(name = "current_load", nullable = false)
    private int currentLoad = 0;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity = 5;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    public AgentProfile() {
    }

    public AgentProfile(Long id, AppUser user, String department, String specialization, int currentLoad, int maxCapacity, boolean isAvailable) {
        this.id = id;
        this.user = user;
        this.department = department != null ? department : "General Support";
        this.specialization = specialization;
        this.currentLoad = currentLoad;
        this.maxCapacity = maxCapacity;
        this.isAvailable = isAvailable;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
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
}
