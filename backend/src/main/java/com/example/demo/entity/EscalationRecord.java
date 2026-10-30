package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "escalation_records")
public class EscalationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalated_by_id", nullable = false)
    private AppUser escalatedBy;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "escalation_level", nullable = false)
    private int escalationLevel = 1;

    @Column(name = "is_resolved", nullable = false)
    private boolean isResolved = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public EscalationRecord() {
    }

    public EscalationRecord(Long id, SupportTicket ticket, AppUser escalatedBy, String reason, int escalationLevel, boolean isResolved, LocalDateTime escalatedAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.ticket = ticket;
        this.escalatedBy = escalatedBy;
        this.reason = reason;
        this.escalationLevel = escalationLevel;
        this.isResolved = isResolved;
        this.escalatedAt = escalatedAt;
        this.resolvedAt = resolvedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.escalatedAt == null) {
            this.escalatedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SupportTicket getTicket() {
        return ticket;
    }

    public void setTicket(SupportTicket ticket) {
        this.ticket = ticket;
    }

    public AppUser getEscalatedBy() {
        return escalatedBy;
    }

    public void setEscalatedBy(AppUser escalatedBy) {
        this.escalatedBy = escalatedBy;
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
