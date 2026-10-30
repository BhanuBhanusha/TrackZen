package com.example.demo.service;

import com.example.demo.dto.StatusUpdateRequestDto;
import com.example.demo.dto.TicketRequestDto;
import com.example.demo.dto.TicketResponseDto;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessValidationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final SupportTicketRepository ticketRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final TicketAssignmentRepository assignmentRepository;
    private final TicketCommentRepository commentRepository;
    private final EscalationRecordRepository escalationRepository;

    private static final Map<Status, Set<Status>> VALID_TRANSITIONS = new EnumMap<>(Status.class);

    static {
        VALID_TRANSITIONS.put(Status.OPEN, Set.of(Status.IN_PROGRESS, Status.CLOSED, Status.ESCALATED));
        VALID_TRANSITIONS.put(Status.IN_PROGRESS, Set.of(Status.RESOLVED, Status.ESCALATED, Status.OPEN));
        VALID_TRANSITIONS.put(Status.ESCALATED, Set.of(Status.IN_PROGRESS, Status.CLOSED));
        VALID_TRANSITIONS.put(Status.RESOLVED, Set.of(Status.CLOSED, Status.OPEN));
        VALID_TRANSITIONS.put(Status.CLOSED, Collections.emptySet());
    }

    public TicketService(SupportTicketRepository ticketRepository,
                         AgentProfileRepository agentProfileRepository,
                         TicketAssignmentRepository assignmentRepository,
                         TicketCommentRepository commentRepository,
                         EscalationRecordRepository escalationRepository) {
        this.ticketRepository = ticketRepository;
        this.agentProfileRepository = agentProfileRepository;
        this.assignmentRepository = assignmentRepository;
        this.commentRepository = commentRepository;
        this.escalationRepository = escalationRepository;
    }

    @Transactional
    public TicketResponseDto createTicket(TicketRequestDto dto, AppUser creator) {
        SupportTicket ticket = new SupportTicket();
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setCategory(dto.getCategory());
        ticket.setPriority(dto.getPriority());
        ticket.setStatus(Status.OPEN);
        ticket.setCreator(creator);

        LocalDateTime now = LocalDateTime.now();
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        long slaHours;
        switch (dto.getPriority()) {
            case CRITICAL -> slaHours = 4;
            case HIGH -> slaHours = 12;
            case MEDIUM -> slaHours = 24;
            case LOW -> slaHours = 72;
            default -> slaHours = 24;
        }
        ticket.setSlaDeadline(now.plusHours(slaHours));

        SupportTicket savedTicket = ticketRepository.save(ticket);
        return mapToDto(savedTicket);
    }

    @Transactional
    public TicketResponseDto updateStatus(Long ticketId, StatusUpdateRequestDto dto) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        Status currentStatus = ticket.getStatus();
        Status targetStatus = dto.getStatus();

        Set<Status> allowedTransitions = VALID_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        if (!allowedTransitions.contains(targetStatus)) {
            throw new BusinessValidationException(
                    "Invalid status transition from " + currentStatus + " to " + targetStatus
            );
        }

        if (targetStatus == Status.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (currentStatus == Status.RESOLVED && targetStatus == Status.OPEN) {
            ticket.setResolvedAt(null);
        }

        ticket.setStatus(targetStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        SupportTicket updatedTicket = ticketRepository.save(ticket);
        return mapToDto(updatedTicket);
    }

    public TicketResponseDto getTicketById(Long id) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
        return mapToDto(ticket);
    }

    public List<TicketResponseDto> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TicketResponseDto> getTicketsByAgent(Long userId) {
        AgentProfile agent = agentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found for user id: " + userId));

        List<TicketAssignment> assignments = assignmentRepository.findByAgentIdAndIsActiveTrue(agent.getId());
        return assignments.stream()
                .map(TicketAssignment::getTicket)
                .filter(Objects::nonNull)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TicketResponseDto> getTicketsByCreator(Long creatorId) {
        return ticketRepository.findByCreatorId(creatorId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TicketResponseDto> getTicketsByStatus(Status status) {
        return ticketRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TicketResponseDto> getTicketsAtSlaRisk() {
        LocalDateTime deadline = LocalDateTime.now().plusHours(24);
        return ticketRepository.findTicketsAtSlaRisk(deadline).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getTicketStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        long total = ticketRepository.count();
        stats.put("TOTAL", total);
        for (Status status : Status.values()) {
            stats.put(status.name(), ticketRepository.countByStatus(status));
        }
        return stats;
    }

    public TicketResponseDto mapToDto(SupportTicket ticket) {
        String assignedAgentName = null;
        Optional<TicketAssignment> activeAssignment = assignmentRepository.findByTicketIdAndIsActiveTrue(ticket.getId());
        if (activeAssignment.isPresent() && activeAssignment.get().getAgent() != null && activeAssignment.get().getAgent().getUser() != null) {
            assignedAgentName = activeAssignment.get().getAgent().getUser().getFullName();
        }

        int commentCount = (int) commentRepository.countByTicketId(ticket.getId());
        int escalationCount = (int) escalationRepository.countByTicketId(ticket.getId());

        return new TicketResponseDto(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCreator() != null ? ticket.getCreator().getFullName() : null,
                ticket.getCreator() != null ? ticket.getCreator().getId() : null,
                assignedAgentName,
                ticket.getSlaDeadline(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt(),
                commentCount,
                escalationCount
        );
    }
}
