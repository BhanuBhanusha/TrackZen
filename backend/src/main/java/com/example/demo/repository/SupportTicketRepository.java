package com.example.demo.repository;

import com.example.demo.entity.Priority;
import com.example.demo.entity.Status;
import com.example.demo.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByStatus(Status status);

    List<SupportTicket> findByCreatorId(Long creatorId);

    List<SupportTicket> findByPriority(Priority priority);

    @Query("SELECT t FROM SupportTicket t WHERE t.slaDeadline < :deadline AND t.status NOT IN (com.example.demo.entity.Status.RESOLVED, com.example.demo.entity.Status.CLOSED)")
    List<SupportTicket> findTicketsAtSlaRisk(@Param("deadline") LocalDateTime deadline);

    long countByStatus(Status status);

    long countByCreatorId(Long creatorId);

    @Query("SELECT t FROM SupportTicket t WHERE t.category = :category AND t.status <> com.example.demo.entity.Status.CLOSED")
    List<SupportTicket> findOpenByCategory(@Param("category") String category);
}
