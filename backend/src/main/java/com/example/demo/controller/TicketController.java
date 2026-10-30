package com.example.demo.controller;

import com.example.demo.dto.StatusUpdateRequestDto;
import com.example.demo.dto.TicketRequestDto;
import com.example.demo.dto.TicketResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Role;
import com.example.demo.entity.Status;
import com.example.demo.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<TicketResponseDto> createTicket(
            @Valid @RequestBody TicketRequestDto dto,
            @AuthenticationPrincipal AppUser creator) {
        TicketResponseDto response = ticketService.createTicket(dto, creator);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponseDto>> getTickets(@AuthenticationPrincipal AppUser currentUser) {
        if (currentUser.getRole() == Role.ROLE_CUSTOMER) {
            return ResponseEntity.ok(ticketService.getTicketsByCreator(currentUser.getId()));
        } else if (currentUser.getRole() == Role.ROLE_SUPPORT_AGENT) {
            List<TicketResponseDto> assigned = ticketService.getTicketsByAgent(currentUser.getId());
            List<TicketResponseDto> created = ticketService.getTicketsByCreator(currentUser.getId());
            java.util.Map<Long, TicketResponseDto> combined = new java.util.LinkedHashMap<>();
            for (TicketResponseDto t : assigned) combined.put(t.getId(), t);
            for (TicketResponseDto t : created) combined.put(t.getId(), t);
            return ResponseEntity.ok(new java.util.ArrayList<>(combined.values()));
        } else {
            // ROLE_SUPERVISOR receives all tickets
            return ResponseEntity.ok(ticketService.getAllTickets());
        }
    }

    @GetMapping("/sla-risk")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPORT_AGENT', 'ROLE_SUPERVISOR')")
    public ResponseEntity<List<TicketResponseDto>> getTicketsAtSlaRisk(@AuthenticationPrincipal AppUser currentUser) {
        List<TicketResponseDto> allRisk = ticketService.getTicketsAtSlaRisk();
        if (currentUser.getRole() == Role.ROLE_SUPPORT_AGENT) {
            return ResponseEntity.ok(allRisk.stream()
                    .filter(t -> currentUser.getFullName().equals(t.getAssignedAgentName()))
                    .collect(java.util.stream.Collectors.toList()));
        }
        return ResponseEntity.ok(allRisk);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPORT_AGENT', 'ROLE_SUPERVISOR')")
    public ResponseEntity<Map<String, Long>> getTicketStats(@AuthenticationPrincipal AppUser currentUser) {
        if (currentUser.getRole() == Role.ROLE_SUPPORT_AGENT) {
            List<TicketResponseDto> assigned = ticketService.getTicketsByAgent(currentUser.getId());
            Map<String, Long> stats = new java.util.LinkedHashMap<>();
            stats.put("TOTAL", (long) assigned.size());
            for (Status status : Status.values()) {
                stats.put(status.name(), assigned.stream().filter(t -> t.getStatus() == status).count());
            }
            return ResponseEntity.ok(stats);
        }
        return ResponseEntity.ok(ticketService.getTicketStats());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPORT_AGENT', 'ROLE_SUPERVISOR')")
    public ResponseEntity<List<TicketResponseDto>> getTicketsByStatus(@PathVariable Status status) {
        return ResponseEntity.ok(ticketService.getTicketsByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDto> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPORT_AGENT', 'ROLE_SUPERVISOR')")
    public ResponseEntity<TicketResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequestDto dto) {
        return ResponseEntity.ok(ticketService.updateStatus(id, dto));
    }
}
