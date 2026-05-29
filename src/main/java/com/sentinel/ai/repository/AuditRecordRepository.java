package com.sentinel.ai.repository;

import com.sentinel.ai.model.entity.AuditRecord;
import com.sentinel.ai.model.enums.AuditVerdict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, UUID> {

    Page<AuditRecord> findByVerdictOrderByCreatedAtDesc(AuditVerdict verdict, Pageable pageable);

    Page<AuditRecord> findByCreatedAtBetweenOrderByCreatedAtDesc(
            Instant startTime, Instant endTime, Pageable pageable);

    long countByVerdict(AuditVerdict verdict);
}
