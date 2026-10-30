package com.example.demo.service;

import com.example.demo.dto.EscalationRequestDto;
import com.example.demo.dto.EscalationResponseDto;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessValidationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.EscalationRecordRepository;
import com.example.demo.repository.SupportTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EscalationService {

    private final EscalationRecordRepository escalationRepository;
    private final SupportTicketRepository ticketRepository;

    public EscalationService(EscalationRecordRepository escalationRepository, SupportTicketRepository ticketRepository) {
        this.escalationRepository = escalationRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public EscalationResponseDto escalateTicket(EscalationRequestDto dto, AppUser escalatedBy) {
        SupportTicket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + dto.getTicketId()));

        if (ticket.getStatus() == Status.CLOSED) {
            throw new BusinessValidationException("Cannot escalate a CLOSED ticket");
        }

        int currentCount = (int) escalationRepository.countByTicketId(ticket.getId());
        int nextLevel = currentCount + 1;

        if (nextLevel > 3) {
            throw new BusinessValidationException("Maximum escalation level (3) reached for this ticket");
        }

        // Auto-bump priority if nextLevel >= 2 and priority is not already CRITICAL
        if (nextLevel >= 2) {
            Priority currentPriority = ticket.getPriority();
            if (currentPriority == Priority.LOW) {
                ticket.setPriority(Priority.MEDIUM);
            } else if (currentPriority == Priority.MEDIUM) {
                ticket.setPriority(Priority.HIGH);
            } else if (currentPriority == Priority.HIGH) {
                ticket.setPriority(Priority.CRITICAL);
            }
        }

        ticket.setStatus(Status.ESCALATED);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        EscalationRecord record = new EscalationRecord();
        record.setTicket(ticket);
        record.setEscalatedBy(escalatedBy);
        record.setReason(dto.getReason());
        record.setEscalationLevel(nextLevel);
        record.setResolved(false);
        record.setEscalatedAt(LocalDateTime.now());

        EscalationRecord saved = escalationRepository.save(record);
        return mapToDto(saved);
    }

    @Transactional
    public EscalationResponseDto resolveEscalation(Long escalationId) {
        EscalationRecord record = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new ResourceNotFoundException("Escalation record not found with id: " + escalationId));

        if (record.isResolved()) {
            throw new BusinessValidationException("Escalation is already resolved");
        }

        record.setResolved(true);
        record.setResolvedAt(LocalDateTime.now());
        EscalationRecord saved = escalationRepository.save(record);

        SupportTicket ticket = record.getTicket();
        if (ticket != null) {
            ticket.setStatus(Status.IN_PROGRESS);
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketRepository.save(ticket);
        }

        return mapToDto(saved);
    }

    public List<EscalationResponseDto> getEscalationsByTicket(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }
        return escalationRepository.findByTicketId(ticketId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<EscalationResponseDto> getAllUnresolved() {
        return escalationRepository.findByIsResolvedFalse().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<EscalationResponseDto> getAllEscalations() {
        return escalationRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private EscalationResponseDto mapToDto(EscalationRecord record) {
        Long ticketId = record.getTicket() != null ? record.getTicket().getId() : null;
        String ticketTitle = record.getTicket() != null ? record.getTicket().getTitle() : null;
        String escalatedByName = record.getEscalatedBy() != null ? record.getEscalatedBy().getFullName() : null;

        return new EscalationResponseDto(
                record.getId(),
                ticketId,
                ticketTitle,
                escalatedByName,
                record.getReason(),
                record.getEscalationLevel(),
                record.isResolved(),
                record.getEscalatedAt(),
                record.getResolvedAt()
        );
    }
}
