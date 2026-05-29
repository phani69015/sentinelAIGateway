package com.sentinel.ai.service.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.ai.config.LlmProviderConfig;
import com.sentinel.ai.model.dto.AuditDecisionDto;
import com.sentinel.ai.model.dto.LlmResponseDto;
import com.sentinel.ai.model.enums.ViolationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scans LLM responses against SEC/FINRA compliance rules.
 * Rules are loaded from a configurable JSON file.
 */
@Slf4j
@Service
public class ComplianceScanner {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final LlmProviderConfig config;
    private List<ComplianceRuleDefinition> rules = new ArrayList<>();

    public ComplianceScanner(ObjectMapper objectMapper, ResourceLoader resourceLoader, LlmProviderConfig config) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.config = config;
    }

    @PostConstruct
    public void loadRules() {
        try {
            var resource = resourceLoader.getResource(config.getCompliance().getRulesPath());
            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                JsonNode rulesNode = root.get("rules");
                rules = objectMapper.convertValue(rulesNode, new TypeReference<>() {});
                log.info("Loaded {} compliance rules", rules.size());
            }
        } catch (Exception e) {
            log.error("Failed to load compliance rules", e);
            rules = new ArrayList<>();
        }
    }

    /**
     * Scan both LLM responses for compliance violations.
     */
    public ComplianceScanResult scan(LlmResponseDto responseA, LlmResponseDto responseB) {
        log.debug("Running compliance scan against {} rules", rules.size());

        List<AuditDecisionDto.ViolationDto> violations = new ArrayList<>();

        violations.addAll(scanResponse(responseA));
        violations.addAll(scanResponse(responseB));

        double maxSeverity = violations.stream()
                .mapToDouble(AuditDecisionDto.ViolationDto::getSeverity)
                .max()
                .orElse(0.0);

        log.info("Compliance scan complete: violations={}, maxSeverity={}", violations.size(), maxSeverity);

        return new ComplianceScanResult(violations, maxSeverity);
    }

    private List<AuditDecisionDto.ViolationDto> scanResponse(LlmResponseDto response) {
        List<AuditDecisionDto.ViolationDto> violations = new ArrayList<>();
        String text = response.getText();

        for (ComplianceRuleDefinition rule : rules) {
            if (rule.patterns != null) {
                // Pattern-match rules
                for (String patternStr : rule.patterns) {
                    Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                    var matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        violations.add(AuditDecisionDto.ViolationDto.builder()
                                .type(ViolationType.valueOf(rule.violationType))
                                .description(rule.description)
                                .severity(rule.severity)
                                .offendingText(matcher.group())
                                .sourceProvider(response.getProvider().name())
                                .build());
                        break; // One match per rule per response is sufficient
                    }
                }
            }

            if (rule.requiresDisclaimer != null && rule.requiresDisclaimer) {
                // Check if advice is given without disclaimer
                boolean givesAdvice = false;
                if (rule.triggerPatterns != null) {
                    for (String triggerPattern : rule.triggerPatterns) {
                        if (Pattern.compile(triggerPattern, Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                            givesAdvice = true;
                            break;
                        }
                    }
                }

                if (givesAdvice) {
                    boolean hasDisclaimer = false;
                    if (rule.disclaimerPatterns != null) {
                        for (String disclaimerPattern : rule.disclaimerPatterns) {
                            if (Pattern.compile(disclaimerPattern, Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                                hasDisclaimer = true;
                                break;
                            }
                        }
                    }

                    if (!hasDisclaimer) {
                        violations.add(AuditDecisionDto.ViolationDto.builder()
                                .type(ViolationType.MISSING_DISCLAIMER)
                                .description(rule.description)
                                .severity(rule.severity)
                                .offendingText("Advice given without required disclaimer")
                                .sourceProvider(response.getProvider().name())
                                .build());
                    }
                }
            }
        }

        return violations;
    }

    public record ComplianceScanResult(List<AuditDecisionDto.ViolationDto> violations, double maxSeverity) {}

    // Internal rule definition matching the JSON structure
    private static class ComplianceRuleDefinition {
        public String id;
        public String name;
        public String description;
        public double severity;
        public String violationType;
        public List<String> patterns;
        public Boolean requiresDisclaimer;
        public List<String> triggerPatterns;
        public List<String> disclaimerPatterns;
        public Boolean requiresBalancedInfo;
        public List<String> benefitPatterns;
        public List<String> riskPatterns;
        public Boolean requiresConsistencyCheck;
    }
}
