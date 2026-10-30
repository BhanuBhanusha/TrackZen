package com.example.demo.controller;

import com.example.demo.dto.AgentProfileResponseDto;
import com.example.demo.service.AgentProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentProfileService agentProfileService;

    public AgentController(AgentProfileService agentProfileService) {
        this.agentProfileService = agentProfileService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public ResponseEntity<List<AgentProfileResponseDto>> getAllAgents() {
        return ResponseEntity.ok(agentProfileService.getAllAgents());
    }

    @GetMapping("/workload")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public ResponseEntity<List<AgentProfileResponseDto>> getWorkloadSummary() {
        return ResponseEntity.ok(agentProfileService.getWorkloadSummary());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_SUPPORT_AGENT')")
    public ResponseEntity<AgentProfileResponseDto> getAgentById(@PathVariable Long id) {
        return ResponseEntity.ok(agentProfileService.getAgentById(id));
    }

    @PutMapping("/{id}/toggle-availability")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public ResponseEntity<AgentProfileResponseDto> toggleAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(agentProfileService.toggleAvailability(id));
    }

    @PostMapping("/{id}/recalculate")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public ResponseEntity<AgentProfileResponseDto> recalculateLoad(@PathVariable Long id) {
        return ResponseEntity.ok(agentProfileService.recalculateLoad(id));
    }
}
