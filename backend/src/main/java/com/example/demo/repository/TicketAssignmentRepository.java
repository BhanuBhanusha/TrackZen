package com.example.demo.repository;

import com.example.demo.entity.TicketAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketAssignmentRepository extends JpaRepository<TicketAssignment, Long> {

    Optional<TicketAssignment> findByTicketIdAndIsActiveTrue(Long ticketId);

    List<TicketAssignment> findByAgentIdAndIsActiveTrue(Long agentId);

    long countByAgentIdAndIsActiveTrue(Long agentId);
}
