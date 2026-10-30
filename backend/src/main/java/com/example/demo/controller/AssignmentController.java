package com.example.demo.controller;

import com.example.demo.dto.AssignmentRequestDto;
import com.example.demo.entity.AppUser;
import com.example.demo.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
@PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> assignTicket(
            @Valid @RequestBody AssignmentRequestDto dto,
            @AuthenticationPrincipal AppUser assignedBy) {
        assignmentService.assignTicket(dto, assignedBy);
        return ResponseEntity.ok(Collections.singletonMap("message", "Ticket assigned successfully"));
    }

    @PostMapping("/auto-assign/{ticketId}")
    public ResponseEntity<Map<String, String>> autoAssignTicket(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal AppUser assignedBy) {
        String resultMessage = assignmentService.autoAssignTicket(ticketId, assignedBy);
        return ResponseEntity.ok(Collections.singletonMap("message", resultMessage));
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Map<String, String>> unassignTicket(@PathVariable Long ticketId) {
        assignmentService.unassignTicket(ticketId);
        return ResponseEntity.ok(Collections.singletonMap("message", "Ticket unassigned successfully"));
    }
}
