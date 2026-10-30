package com.example.demo.repository;

import com.example.demo.entity.AgentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {

    Optional<AgentProfile> findByUserId(Long userId);

    @Query("SELECT a FROM AgentProfile a WHERE a.currentLoad < a.maxCapacity AND a.isAvailable = true")
    List<AgentProfile> findAvailableAgents();

    @Query("SELECT a FROM AgentProfile a WHERE a.currentLoad < a.maxCapacity AND a.isAvailable = true ORDER BY a.currentLoad ASC")
    List<AgentProfile> findLeastLoadedAgent();
}
