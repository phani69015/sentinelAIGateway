package com.sentinel.ai.model.entity;

import com.sentinel.ai.model.enums.AuditVerdict;
import com.sentinel.ai.model.enums.ProviderType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "audit_records")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData;

    @Column(name = "response_a", nullable = false, columnDefinition = "TEXT")
    private String responseA;

    @Column(name = "response_a_provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProviderType responseAProvider;

    @Column(name = "response_a_model")
    private String responseAModel;

    @Column(name = "response_b", nullable = false, columnDefinition = "TEXT")
    private String responseB;

    @Column(name = "response_b_provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProviderType responseBProvider;

    @Column(name = "response_b_model")
    private String responseBModel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditVerdict verdict;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Column(name = "consistency_score", nullable = false)
    private Double consistencyScore;

    @Column(name = "audit_reasoning", nullable = false, columnDefinition = "TEXT")
    private String auditReasoning;

    @Column(name = "delivered_response", columnDefinition = "TEXT")
    private String deliveredResponse;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "auditRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplianceViolation> violations;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
