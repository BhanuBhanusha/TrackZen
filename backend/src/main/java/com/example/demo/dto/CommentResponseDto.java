package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class CommentResponseDto {

    private Long id;
    private Long ticketId;
    private String authorName;
    private String authorRole;
    private String content;

    @JsonProperty("isInternal")
    private boolean isInternal;

    private LocalDateTime createdAt;

    public CommentResponseDto() {
    }

    public CommentResponseDto(Long id, Long ticketId, String authorName, String authorRole, String content, boolean isInternal, LocalDateTime createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.content = content;
        this.isInternal = isInternal;
        this.createdAt = createdAt;
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

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
