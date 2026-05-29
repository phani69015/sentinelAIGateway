package com.sentinel.ai.model.dto;

import com.sentinel.ai.model.enums.AuditVerdict;
import com.sentinel.ai.model.enums.ProviderType;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecordDto {

    private UUID id;
    private Instant timestamp;
    private String query;
    private LlmResponseDto responseA;
    private LlmResponseDto responseB;
    private AuditDecisionDto auditDecision;
    private String deliveredResponse;
    private AuditVerdict verdict;
    private long latencyMs;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LlmResponseDto {
        private ProviderType provider;
        private String text;
        private String model;
    }
}
