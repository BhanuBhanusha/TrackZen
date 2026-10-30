package com.example.demo.service;

import com.example.demo.dto.AssignmentRequestDto;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessValidationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AgentProfileRepository;
import com.example.demo.repository.SupportTicketRepository;
import com.example.demo.repository.TicketAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    private final SupportTicketRepository ticketRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final TicketAssignmentRepository assignmentRepository;

    public AssignmentService(SupportTicketRepository ticketRepository,
                             AgentProfileRepository agentProfileRepository,
                             TicketAssignmentRepository assignmentRepository) {
        this.ticketRepository = ticketRepository;
        this.agentProfileRepository = agentProfileRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public void assignTicket(AssignmentRequestDto dto, AppUser assignedBy) {
        SupportTicket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + dto.getTicketId()));

        if (ticket.getStatus() == Status.CLOSED || ticket.getStatus() == Status.RESOLVED) {
            throw new BusinessValidationException("Cannot assign a ticket that is " + ticket.getStatus());
        }

        AgentProfile agent = agentProfileRepository.findById(dto.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found with id: " + dto.getAgentId()));

        if (!agent.isAvailable()) {
            throw new BusinessValidationException("Agent is not available");
        }

        if (agent.getCurrentLoad() >= agent.getMaxCapacity()) {
            throw new BusinessValidationException("Agent is at maximum capacity");
        }

        // Deactivate existing active assignment and decrement old agent load
        Optional<TicketAssignment> currentAssignmentOpt = assignmentRepository.findByTicketIdAndIsActiveTrue(ticket.getId());
        if (currentAssignmentOpt.isPresent()) {
            TicketAssignment currentAssignment = currentAssignmentOpt.get();
            currentAssignment.setActive(false);
            assignmentRepository.save(currentAssignment);

            AgentProfile oldAgent = currentAssignment.getAgent();
            if (oldAgent != null) {
                oldAgent.setCurrentLoad(Math.max(0, oldAgent.getCurrentLoad() - 1));
                agentProfileRepository.save(oldAgent);
            }
        }

        // Create new TicketAssignment
        TicketAssignment newAssignment = new TicketAssignment();
        newAssignment.setTicket(ticket);
        newAssignment.setAgent(agent);
        newAssignment.setAssignedBy(assignedBy);
        newAssignment.setAssignedAt(LocalDateTime.now());
        newAssignment.setActive(true);
        assignmentRepository.save(newAssignment);

        // Increment new agent load
        agent.setCurrentLoad(agent.getCurrentLoad() + 1);
        agentProfileRepository.save(agent);

        // Transition ticket to IN_PROGRESS if OPEN or ESCALATED
        if (ticket.getStatus() == Status.OPEN || ticket.getStatus() == Status.ESCALATED) {
            ticket.setStatus(Status.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

    @Transactional
    public String autoAssignTicket(Long ticketId, AppUser assignedBy) {
        List<AgentProfile> availableAgents = agentProfileRepository.findLeastLoadedAgent();
        if (availableAgents.isEmpty()) {
            throw new BusinessValidationException("No available agents found for auto-assignment");
        }

        AgentProfile leastLoadedAgent = availableAgents.get(0);
        assignTicket(new AssignmentRequestDto(ticketId, leastLoadedAgent.getId()), assignedBy);

        String agentName = leastLoadedAgent.getUser() != null ? leastLoadedAgent.getUser().getFullName() : "Agent #" + leastLoadedAgent.getId();
        return "Ticket successfully auto-assigned to agent: " + agentName;
    }

    @Transactional
    public void unassignTicket(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        Optional<TicketAssignment> activeAssignmentOpt = assignmentRepository.findByTicketIdAndIsActiveTrue(ticketId);
        if (activeAssignmentOpt.isPresent()) {
            TicketAssignment activeAssignment = activeAssignmentOpt.get();
            activeAssignment.setActive(false);
            assignmentRepository.save(activeAssignment);

            AgentProfile agent = activeAssignment.getAgent();
            if (agent != null) {
                agent.setCurrentLoad(Math.max(0, agent.getCurrentLoad() - 1));
                agentProfileRepository.save(agent);
            }
        }

        ticket.setStatus(Status.OPEN);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
    }
}
