package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class EscalationResponseDto {

    private Long id;
    private Long ticketId;
    private String ticketTitle;
    private String escalatedByName;
    private String reason;
    private int escalationLevel;

    @JsonProperty("isResolved")
    private boolean isResolved;

    private LocalDateTime escalatedAt;
    private LocalDateTime resolvedAt;

    public EscalationResponseDto() {
    }

    public EscalationResponseDto(Long id, Long ticketId, String ticketTitle, String escalatedByName, String reason, int escalationLevel, boolean isResolved, LocalDateTime escalatedAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.ticketTitle = ticketTitle;
        this.escalatedByName = escalatedByName;
        this.reason = reason;
        this.escalationLevel = escalationLevel;
        this.isResolved = isResolved;
        this.escalatedAt = escalatedAt;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketTitle() {
        return ticketTitle;
    }

    public void setTicketTitle(String ticketTitle) {
        this.ticketTitle = ticketTitle;
    }

    public String getEscalatedByName() {
        return escalatedByName;
    }

    public void setEscalatedByName(String escalatedByName) {
        this.escalatedByName = escalatedByName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(int escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
