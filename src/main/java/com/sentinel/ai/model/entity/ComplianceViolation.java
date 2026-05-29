package com.sentinel.ai.model.entity;

import com.sentinel.ai.model.enums.ViolationType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "compliance_violations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_record_id", nullable = false)
    private AuditRecord auditRecord;

    @Column(name = "violation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ViolationType violationType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double severity;

    @Column(name = "offending_text", columnDefinition = "TEXT")
    private String offendingText;

    @Column(name = "source_provider")
    @Enumerated(EnumType.STRING)
    private com.sentinel.ai.model.enums.ProviderType sourceProvider;
}
