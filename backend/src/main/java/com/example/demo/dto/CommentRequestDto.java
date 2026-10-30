package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class CommentRequestDto {

    @NotBlank(message = "Content is required")
    private String content;

    @JsonProperty("isInternal")
    private boolean isInternal;

    public CommentRequestDto() {
    }

    public CommentRequestDto(String content, boolean isInternal) {
        this.content = content;
        this.isInternal = isInternal;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isInternal() {
        return isInternal;
    }

    public void setInternal(boolean internal) {
        isInternal = internal;
    }
}
