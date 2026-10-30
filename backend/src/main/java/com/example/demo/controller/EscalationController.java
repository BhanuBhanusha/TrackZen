package com.example.demo.controller;

import com.example.demo.dto.EscalationRequestDto;
import com.example.demo.dto.EscalationResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.service.EscalationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/escalations")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_SUPPORT_AGENT')")
    public ResponseEntity<EscalationResponseDto> escalateTicket(
            @Valid @RequestBody EscalationRequestDto dto,
            @AuthenticationPrincipal AppUser user) {
        EscalationResponseDto response = escalationService.escalateTicket(dto, user);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public ResponseEntity<EscalationResponseDto> resolveEscalation(@PathVariable Long id) {
        EscalationResponseDto response = escalationService.resolveEscalation(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPORT_AGENT', 'ROLE_SUPERVISOR')")
    public ResponseEntity<List<EscalationResponseDto>> getEscalationsByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(escalationService.getEscalationsByTicket(ticketId));
    }

    @GetMapping("/unresolved")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public ResponseEntity<List<EscalationResponseDto>> getAllUnresolved() {
        return ResponseEntity.ok(escalationService.getAllUnresolved());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public ResponseEntity<List<EscalationResponseDto>> getAllEscalations() {
        return ResponseEntity.ok(escalationService.getAllEscalations());
    }
}
