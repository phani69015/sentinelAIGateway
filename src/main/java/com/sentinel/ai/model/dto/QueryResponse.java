package com.sentinel.ai.model.dto;

import com.sentinel.ai.model.enums.AuditVerdict;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {

    private String response;
    private UUID auditId;
    private AuditVerdict verdict;
    private Double confidence;
    private ResponseMetadata metadata;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseMetadata {
        private boolean respondersAgreed;
        private int complianceChecks;
        private int violationsFound;
        private long latencyMs;
    }
}
