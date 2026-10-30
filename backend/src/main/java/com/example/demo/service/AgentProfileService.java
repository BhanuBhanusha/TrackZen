package com.example.demo.service;

import com.example.demo.dto.AgentProfileResponseDto;
import com.example.demo.entity.AgentProfile;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AgentProfileRepository;
import com.example.demo.repository.TicketAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentProfileService {

    private final AgentProfileRepository agentProfileRepository;
    private final TicketAssignmentRepository assignmentRepository;

    public AgentProfileService(AgentProfileRepository agentProfileRepository,
                               TicketAssignmentRepository assignmentRepository) {
        this.agentProfileRepository = agentProfileRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<AgentProfileResponseDto> getAllAgents() {
        return agentProfileRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AgentProfileResponseDto getAgentById(Long id) {
        AgentProfile profile = agentProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found with id: " + id));
        return mapToDto(profile);
    }

    @Transactional
    public AgentProfileResponseDto recalculateLoad(Long agentId) {
        AgentProfile profile = agentProfileRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found with id: " + agentId));

        int activeCount = (int) assignmentRepository.countByAgentIdAndIsActiveTrue(agentId);
        profile.setCurrentLoad(activeCount);
        AgentProfile saved = agentProfileRepository.save(profile);
        return mapToDto(saved);
    }

    @Transactional
    public AgentProfileResponseDto updateAgentProfile(Long id, int maxCapacity, boolean isAvailable, String department, String specialization) {
        AgentProfile profile = agentProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found with id: " + id));

        profile.setMaxCapacity(maxCapacity);
        profile.setAvailable(isAvailable);
        if (department != null) {
            profile.setDepartment(department);
        }
        if (specialization != null) {
            profile.setSpecialization(specialization);
        }
        AgentProfile saved = agentProfileRepository.save(profile);
        return mapToDto(saved);
    }

    @Transactional
    public AgentProfileResponseDto toggleAvailability(Long id) {
        AgentProfile profile = agentProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found with id: " + id));

        profile.setAvailable(!profile.isAvailable());
        AgentProfile saved = agentProfileRepository.save(profile);
        return mapToDto(saved);
    }

    public List<AgentProfileResponseDto> getWorkloadSummary() {
        return agentProfileRepository.findAll().stream()
                .map(this::mapToDto)
                .sorted(Comparator.comparingDouble(AgentProfileResponseDto::getLoadPercentage).reversed())
                .collect(Collectors.toList());
    }

    private AgentProfileResponseDto mapToDto(AgentProfile profile) {
        double percentage = 0.0;
        if (profile.getMaxCapacity() > 0) {
            double raw = ((double) profile.getCurrentLoad() / (double) profile.getMaxCapacity()) * 100.0;
            percentage = BigDecimal.valueOf(raw)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        Long userId = profile.getUser() != null ? profile.getUser().getId() : null;
        String fullName = profile.getUser() != null ? profile.getUser().getFullName() : null;
        String email = profile.getUser() != null ? profile.getUser().getEmail() : null;

        return new AgentProfileResponseDto(
                profile.getId(),
                userId,
                fullName,
                email,
                profile.getDepartment(),
                profile.getSpecialization(),
                profile.getCurrentLoad(),
                profile.getMaxCapacity(),
                profile.isAvailable(),
                percentage
        );
    }
}
