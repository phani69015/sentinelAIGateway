package com.sentinel.ai.model.dto;

import com.sentinel.ai.model.enums.AuditVerdict;
import com.sentinel.ai.model.enums.ViolationType;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDecisionDto {

    private AuditVerdict verdict;
    private Double confidence;
    private Double consistencyScore;
    private List<String> hallucinationFlags;
    private List<ViolationDto> complianceViolations;
    private List<String> toxicityFlags;
    private String reasoning;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViolationDto {
        private ViolationType type;
        private String description;
        private Double severity;
        private String offendingText;
        private String sourceProvider;
    }
}
