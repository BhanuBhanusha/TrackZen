package com.example.demo.service;

import com.example.demo.dto.CommentRequestDto;
import com.example.demo.dto.CommentResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Role;
import com.example.demo.entity.Status;
import com.example.demo.entity.SupportTicket;
import com.example.demo.entity.TicketComment;
import com.example.demo.exception.BusinessValidationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.SupportTicketRepository;
import com.example.demo.repository.TicketCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final TicketCommentRepository commentRepository;
    private final SupportTicketRepository ticketRepository;

    public CommentService(TicketCommentRepository commentRepository, SupportTicketRepository ticketRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public CommentResponseDto addComment(Long ticketId, CommentRequestDto dto, AppUser author) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (ticket.getStatus() == Status.CLOSED) {
            throw new BusinessValidationException("Cannot add comments to a CLOSED ticket");
        }

        boolean isInternal = dto.isInternal();
        if (author.getRole() == Role.ROLE_CUSTOMER) {
            isInternal = false;
        }

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(dto.getContent());
        comment.setInternal(isInternal);
        comment.setCreatedAt(LocalDateTime.now());

        TicketComment saved = commentRepository.save(comment);

        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        return mapToDto(saved);
    }

    public List<CommentResponseDto> getComments(Long ticketId, AppUser viewer) {
        // Ensure ticket exists
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }

        List<TicketComment> comments;
        if (viewer.getRole() == Role.ROLE_CUSTOMER) {
            comments = commentRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(ticketId);
        } else {
            comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        }

        return comments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Long commentId, AppUser requestor) {
        TicketComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        boolean isAuthor = comment.getAuthor() != null && comment.getAuthor().getId().equals(requestor.getId());
        boolean isSupervisor = requestor.getRole() == Role.ROLE_SUPERVISOR;

        if (!isAuthor && !isSupervisor) {
            throw new BusinessValidationException("Unauthorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    private CommentResponseDto mapToDto(TicketComment comment) {
        Long ticketId = comment.getTicket() != null ? comment.getTicket().getId() : null;
        String authorName = comment.getAuthor() != null ? comment.getAuthor().getFullName() : null;
        String authorRole = comment.getAuthor() != null && comment.getAuthor().getRole() != null
                ? comment.getAuthor().getRole().name() : null;

        return new CommentResponseDto(
                comment.getId(),
                ticketId,
                authorName,
                authorRole,
                comment.getContent(),
                comment.isInternal(),
                comment.getCreatedAt()
        );
    }
}
