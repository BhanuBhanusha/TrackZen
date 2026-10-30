package com.example.demo.dto;

import com.example.demo.entity.Status;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequestDto {

    @NotNull(message = "Status is required")
    private Status status;

    public StatusUpdateRequestDto() {
    }

    public StatusUpdateRequestDto(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
