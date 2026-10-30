package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class AssignmentRequestDto {

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    public AssignmentRequestDto() {
    }

    public AssignmentRequestDto(Long ticketId, Long agentId) {
        this.ticketId = ticketId;
        this.agentId = agentId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
}
