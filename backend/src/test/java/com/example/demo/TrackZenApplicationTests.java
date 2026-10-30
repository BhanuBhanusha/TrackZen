package com.example.demo;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessValidationException;
import com.example.demo.repository.AgentProfileRepository;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TrackZenApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private AgentProfileService agentProfileService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private EscalationService escalationService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AgentProfileRepository agentProfileRepository;

    private AppUser customerUser;
    private AppUser agentUser;
    private AppUser supervisorUser;

    @BeforeEach
    void setup() {
        AuthResponseDto customerAuth = authService.register(
                new RegisterRequestDto("cust@trackzen.com", "password123", "Alice Customer", Role.ROLE_CUSTOMER)
        );
        assertNotNull(customerAuth.getToken());
        customerUser = userRepository.findByEmail("cust@trackzen.com").orElseThrow();

        AuthResponseDto agentAuth = authService.register(
                new RegisterRequestDto("agent@trackzen.com", "password123", "Bob Agent", Role.ROLE_SUPPORT_AGENT)
        );
        assertNotNull(agentAuth.getToken());
        agentUser = userRepository.findByEmail("agent@trackzen.com").orElseThrow();

        AuthResponseDto supAuth = authService.register(
                new RegisterRequestDto("sup@trackzen.com", "password123", "Carol Supervisor", Role.ROLE_SUPERVISOR)
        );
        assertNotNull(supAuth.getToken());
        supervisorUser = userRepository.findByEmail("sup@trackzen.com").orElseThrow();
    }

    @Test
    void testAgentProfileAutoCreation() {
        AgentProfile profile = agentProfileRepository.findByUserId(agentUser.getId()).orElse(null);
        assertNotNull(profile);
        assertEquals("General Support", profile.getDepartment());
        assertEquals(5, profile.getMaxCapacity());
        assertEquals(0, profile.getCurrentLoad());
        assertTrue(profile.isAvailable());
    }

    @Test
    void testDuplicateEmailRegistration() {
        assertThrows(BusinessValidationException.class, () ->
                authService.register(new RegisterRequestDto("cust@trackzen.com", "pass", "Duplicate", Role.ROLE_CUSTOMER))
        );
    }

    @Test
    void testTicketCreationAndSlaCalculation() {
        TicketResponseDto ticket = ticketService.createTicket(
                new TicketRequestDto("Cannot login", "Getting 500 error", "AUTH", Priority.CRITICAL),
                customerUser
        );

        assertNotNull(ticket.getId());
        assertEquals(Status.OPEN, ticket.getStatus());
        assertEquals(Priority.CRITICAL, ticket.getPriority());
        assertEquals("Alice Customer", ticket.getCreatorName());
        assertNotNull(ticket.getSlaDeadline());
        assertTrue(ticket.getSlaDeadline().isAfter(ticket.getCreatedAt()));
    }

    @Test
    void testStatusStateMachineTransitions() {
        TicketResponseDto ticket = ticketService.createTicket(
                new TicketRequestDto("Slow UI", "Page takes 10s to load", "PERF", Priority.MEDIUM),
                customerUser
        );

        // Valid transition OPEN -> IN_PROGRESS
        TicketResponseDto inProg = ticketService.updateStatus(ticket.getId(), new StatusUpdateRequestDto(Status.IN_PROGRESS));
        assertEquals(Status.IN_PROGRESS, inProg.getStatus());

        // Valid transition IN_PROGRESS -> RESOLVED
        TicketResponseDto resolved = ticketService.updateStatus(ticket.getId(), new StatusUpdateRequestDto(Status.RESOLVED));
        assertEquals(Status.RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());

        // Valid transition RESOLVED -> OPEN (clears resolvedAt)
        TicketResponseDto reopened = ticketService.updateStatus(ticket.getId(), new StatusUpdateRequestDto(Status.OPEN));
        assertEquals(Status.OPEN, reopened.getStatus());
        assertNull(reopened.getResolvedAt());

        // Valid transition OPEN -> CLOSED
        TicketResponseDto closed = ticketService.updateStatus(ticket.getId(), new StatusUpdateRequestDto(Status.CLOSED));
        assertEquals(Status.CLOSED, closed.getStatus());

        // Invalid transition CLOSED -> OPEN
        assertThrows(BusinessValidationException.class, () ->
                ticketService.updateStatus(ticket.getId(), new StatusUpdateRequestDto(Status.OPEN))
        );
    }

    @Test
    void testAssignmentAndAutoAssignment() {
        TicketResponseDto ticket = ticketService.createTicket(
                new TicketRequestDto("Payment failed", "Card declined repeatedly", "BILLING", Priority.HIGH),
                customerUser
        );

        AgentProfile agentProfile = agentProfileRepository.findByUserId(agentUser.getId()).orElseThrow();

        // Assign ticket
        assignmentService.assignTicket(new AssignmentRequestDto(ticket.getId(), agentProfile.getId()), supervisorUser);

        AgentProfile updatedProfile = agentProfileRepository.findById(agentProfile.getId()).orElseThrow();
        assertEquals(1, updatedProfile.getCurrentLoad());

        TicketResponseDto fetchedTicket = ticketService.getTicketById(ticket.getId());
        assertEquals(Status.IN_PROGRESS, fetchedTicket.getStatus());
        assertEquals("Bob Agent", fetchedTicket.getAssignedAgentName());

        // Unassign ticket
        assignmentService.unassignTicket(ticket.getId());
        updatedProfile = agentProfileRepository.findById(agentProfile.getId()).orElseThrow();
        assertEquals(0, updatedProfile.getCurrentLoad());

        fetchedTicket = ticketService.getTicketById(ticket.getId());
        assertEquals(Status.OPEN, fetchedTicket.getStatus());
    }

    @Test
    void testCommentsVisibilityFiltering() {
        TicketResponseDto ticket = ticketService.createTicket(
                new TicketRequestDto("Bug report", "Feature not working", "BUG", Priority.LOW),
                customerUser
        );

        // Customer adds comment
        commentService.addComment(ticket.getId(), new CommentRequestDto("Customer comment", false), customerUser);

        // Agent adds internal comment
        commentService.addComment(ticket.getId(), new CommentRequestDto("Agent note: check server logs", true), agentUser);

        // Customer viewer only sees 1 comment
        List<CommentResponseDto> customerComments = commentService.getComments(ticket.getId(), customerUser);
        assertEquals(1, customerComments.size());
        assertEquals("Customer comment", customerComments.get(0).getContent());

        // Agent viewer sees both comments
        List<CommentResponseDto> agentComments = commentService.getComments(ticket.getId(), agentUser);
        assertEquals(2, agentComments.size());
    }

    @Test
    void testEscalationAndPriorityBumping() {
        TicketResponseDto ticket = ticketService.createTicket(
                new TicketRequestDto("Service Outage", "Database latency high", "INFRA", Priority.LOW),
                customerUser
        );

        // Escalation 1: level 1, priority stays LOW
        EscalationResponseDto esc1 = escalationService.escalateTicket(
                new EscalationRequestDto(ticket.getId(), "Level 1 escalation"),
                supervisorUser
        );
        assertEquals(1, esc1.getEscalationLevel());
        TicketResponseDto t1 = ticketService.getTicketById(ticket.getId());
        assertEquals(Status.ESCALATED, t1.getStatus());
        assertEquals(Priority.LOW, t1.getPriority());

        // Escalation 2: level 2, priority bumps LOW -> MEDIUM
        EscalationResponseDto esc2 = escalationService.escalateTicket(
                new EscalationRequestDto(ticket.getId(), "Level 2 escalation"),
                supervisorUser
        );
        assertEquals(2, esc2.getEscalationLevel());
        TicketResponseDto t2 = ticketService.getTicketById(ticket.getId());
        assertEquals(Priority.MEDIUM, t2.getPriority());

        // Escalation 3: level 3, priority bumps MEDIUM -> HIGH
        EscalationResponseDto esc3 = escalationService.escalateTicket(
                new EscalationRequestDto(ticket.getId(), "Level 3 escalation"),
                supervisorUser
        );
        assertEquals(3, esc3.getEscalationLevel());
        TicketResponseDto t3 = ticketService.getTicketById(ticket.getId());
        assertEquals(Priority.HIGH, t3.getPriority());

        // Escalation 4: exceeds max level 3 -> throws BusinessValidationException
        assertThrows(BusinessValidationException.class, () ->
                escalationService.escalateTicket(new EscalationRequestDto(ticket.getId(), "Level 4"), supervisorUser)
        );

        // Resolve escalation 3: transitions ticket to IN_PROGRESS
        escalationService.resolveEscalation(esc3.getId());
        TicketResponseDto tResolved = ticketService.getTicketById(ticket.getId());
        assertEquals(Status.IN_PROGRESS, tResolved.getStatus());
    }

    @Test
    void testTicketStats() {
        ticketService.createTicket(new TicketRequestDto("Ticket 1", "Desc", "CAT", Priority.LOW), customerUser);
        Map<String, Long> stats = ticketService.getTicketStats();
        assertNotNull(stats);
        assertTrue(stats.get("TOTAL") >= 1);
        assertTrue(stats.get("OPEN") >= 1);
    }
}
