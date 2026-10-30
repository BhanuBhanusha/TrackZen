package com.example.demo.controller;

import com.example.demo.dto.CommentRequestDto;
import com.example.demo.dto.CommentResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponseDto> addComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody CommentRequestDto dto,
            @AuthenticationPrincipal AppUser author) {
        CommentResponseDto response = commentService.addComment(ticketId, dto, author);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal AppUser viewer) {
        List<CommentResponseDto> comments = commentService.getComments(ticketId, viewer);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPORT_AGENT', 'ROLE_SUPERVISOR')")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AppUser requestor) {
        commentService.deleteComment(commentId, requestor);
        return ResponseEntity.noContent().build();
    }
}
