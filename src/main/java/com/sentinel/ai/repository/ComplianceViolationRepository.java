package com.sentinel.ai.repository;

import com.sentinel.ai.model.entity.ComplianceViolation;
import com.sentinel.ai.model.enums.ViolationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplianceViolationRepository extends JpaRepository<ComplianceViolation, UUID> {

    List<ComplianceViolation> findByAuditRecordId(UUID auditRecordId);

    long countByViolationType(ViolationType type);
}
