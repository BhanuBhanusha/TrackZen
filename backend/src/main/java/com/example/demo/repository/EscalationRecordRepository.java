package com.example.demo.repository;

import com.example.demo.entity.EscalationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationRecordRepository extends JpaRepository<EscalationRecord, Long> {

    List<EscalationRecord> findByTicketId(Long ticketId);

    List<EscalationRecord> findByIsResolvedFalse();

    long countByTicketId(Long ticketId);
}
