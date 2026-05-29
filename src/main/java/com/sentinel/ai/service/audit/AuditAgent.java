package com.sentinel.ai.service.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.ai.config.LlmProviderConfig;
import com.sentinel.ai.model.dto.AuditDecisionDto;
import com.sentinel.ai.model.dto.LlmResponseDto;
import com.sentinel.ai.model.enums.AuditVerdict;
import com.sentinel.ai.model.enums.ProviderType;
import com.sentinel.ai.model.enums.ViolationType;
import com.sentinel.ai.service.llm.LlmProvider;
import com.sentinel.ai.service.llm.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The Audit Agent is the central intelligence that cross-validates
 * two LLM responses using both rule-based checks and LLM-powered reasoning.
 *
 * It combines:
 * 1. Rule-based consistency checking (numerical claims, contradictions)
 * 2. Rule-based compliance scanning (SEC/FINRA pattern matching)
 * 3. Rule-based toxicity filtering
 * 4. LLM-powered hallucination detection and nuanced reasoning
 *
 * The final verdict (PASS/WARN/BLOCK) is determined by aggregating all checks.
 */
@Slf4j
@Service
public class AuditAgent {

    private final ConsistencyChecker consistencyChecker;
    private final HallucinationDetector hallucinationDetector;
    private final ComplianceScanner complianceScanner;
    private final ToxicityFilter toxicityFilter;
    private final List<LlmProvider> llmProviders;
    private final LlmProviderConfig config;
    private final ObjectMapper objectMapper;

    public AuditAgent(
            ConsistencyChecker consistencyChecker,
            HallucinationDetector hallucinationDetector,
            ComplianceScanner complianceScanner,
            ToxicityFilter toxicityFilter,
            List<LlmProvider> llmProviders,
            LlmProviderConfig config,
            ObjectMapper objectMapper) {
        this.consistencyChecker = consistencyChecker;
        this.hallucinationDetector = hallucinationDetector;
        this.complianceScanner = complianceScanner;
        this.toxicityFilter = toxicityFilter;
        this.llmProviders = llmProviders;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluate two LLM responses and produce an audit decision.
     */
    public AuditDecisionDto evaluate(
            LlmResponseDto responseA,
            LlmResponseDto responseB,
            String originalQuery,
            String context) {

        log.info("Audit Agent evaluating responses from {} and {}",
                responseA.getProvider(), responseB.getProvider());

        // Step 1: Rule-based consistency check
        var consistencyResult = consistencyChecker.check(responseA, responseB);

        // Step 2: Rule-based hallucination detection
        var hallucinationResult = hallucinationDetector.detect(responseA, responseB, context);

        // Step 3: Rule-based compliance scan
        var complianceResult = complianceScanner.scan(responseA, responseB);

        // Step 4: Rule-based toxicity filter
        var toxicityResult = toxicityFilter.filter(responseA, responseB);

        // Step 5: LLM-powered deep analysis (the Audit Agent's own reasoning)
        AuditDecisionDto llmAuditDecision = performLlmAudit(
                responseA, responseB, originalQuery, context);

        // Step 6: Aggregate all results into final decision
        return aggregateDecision(
                consistencyResult,
                hallucinationResult,
                complianceResult,
                toxicityResult,
                llmAuditDecision);
    }

    /**
     * Use a third LLM call to perform nuanced cross-validation.
     * This catches issues that rule-based checks might miss.
     */
    private AuditDecisionDto performLlmAudit(
            LlmResponseDto responseA,
            LlmResponseDto responseB,
            String query,
            String context) {

        LlmProvider auditProvider = getAuditProvider();

        if (auditProvider == null || !auditProvider.isAvailable()) {
            log.warn("Audit LLM provider not available, skipping LLM-based audit");
            return AuditDecisionDto.builder()
                    .verdict(AuditVerdict.WARN)
                    .confidence(0.5)
                    .reasoning("LLM-based audit skipped: provider not available")
                    .build();
        }

        try {
            String userPrompt = PromptTemplates.buildAuditUserPrompt(
                    query,
                    responseA.getText(), responseA.getProvider().name(),
                    responseB.getText(), responseB.getProvider().name(),
                    context);

            LlmResponseDto auditResponse = auditProvider.complete(
                    PromptTemplates.AUDIT_AGENT_SYSTEM_PROMPT, userPrompt);

            return parseAuditResponse(auditResponse.getText());

        } catch (Exception e) {
            log.error("LLM-based audit failed, falling back to rule-based only", e);
            return AuditDecisionDto.builder()
                    .verdict(AuditVerdict.WARN)
                    .confidence(0.5)
                    .reasoning("LLM audit failed: " + e.getMessage())
                    .build();
        }
    }

    private LlmProvider getAuditProvider() {
        ProviderType auditProviderType = config.getAudit().getProvider();
        return llmProviders.stream()
                .filter(p -> p.getProviderType() == auditProviderType)
                .findFirst()
                .orElse(llmProviders.stream().filter(LlmProvider::isAvailable).findFirst().orElse(null));
    }

    private AuditDecisionDto parseAuditResponse(String responseText) {
        try {
            // Try to extract JSON from the response (it might be wrapped in markdown)
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);

            AuditVerdict verdict = AuditVerdict.valueOf(root.get("verdict").asText().toUpperCase());
            double confidence = root.has("confidence") ? root.get("confidence").asDouble() : 0.7;
            double consistencyScore = root.has("consistencyScore") ? root.get("consistencyScore").asDouble() : 0.8;
            String reasoning = root.has("reasoning") ? root.get("reasoning").asText() : "";

            List<String> hallucinationFlags = new ArrayList<>();
            if (root.has("hallucinationFlags")) {
                for (JsonNode flag : root.get("hallucinationFlags")) {
                    hallucinationFlags.add(flag.asText());
                }
            }

            List<AuditDecisionDto.ViolationDto> violations = new ArrayList<>();
            if (root.has("complianceViolations")) {
                for (JsonNode violation : root.get("complianceViolations")) {
                    violations.add(AuditDecisionDto.ViolationDto.builder()
                            .type(parseViolationType(violation.get("type").asText()))
                            .description(violation.has("description") ? violation.get("description").asText() : "")
                            .severity(violation.has("severity") ? violation.get("severity").asDouble() : 0.5)
                            .offendingText(violation.has("offendingText") ? violation.get("offendingText").asText() : "")
                            .sourceProvider(violation.has("sourceProvider") ? violation.get("sourceProvider").asText() : "")
                            .build());
                }
            }

            List<String> toxicityFlags = new ArrayList<>();
            if (root.has("toxicityFlags")) {
                for (JsonNode flag : root.get("toxicityFlags")) {
                    toxicityFlags.add(flag.asText());
                }
            }

            return AuditDecisionDto.builder()
                    .verdict(verdict)
                    .confidence(confidence)
                    .consistencyScore(consistencyScore)
                    .hallucinationFlags(hallucinationFlags)
                    .complianceViolations(violations)
                    .toxicityFlags(toxicityFlags)
                    .reasoning(reasoning)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse LLM audit response, defaulting to WARN", e);
            return AuditDecisionDto.builder()
                    .verdict(AuditVerdict.WARN)
                    .confidence(0.5)
                    .reasoning("Failed to parse audit response: " + e.getMessage())
                    .build();
        }
    }

    private String extractJson(String text) {
        // Try to find JSON block in markdown code fence
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private ViolationType parseViolationType(String type) {
        try {
            return ViolationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ViolationType.FACTUAL_INACCURACY;
        }
    }

    /**
     * Aggregate rule-based results with LLM audit into a final decision.
     * The strictest verdict wins.
     */
    private AuditDecisionDto aggregateDecision(
            ConsistencyChecker.ConsistencyResult consistency,
            HallucinationDetector.HallucinationResult hallucination,
            ComplianceScanner.ComplianceScanResult compliance,
            ToxicityFilter.ToxicityResult toxicity,
            AuditDecisionDto llmDecision) {

        List<String> allHallucinationFlags = new ArrayList<>(hallucination.flags());
        if (llmDecision.getHallucinationFlags() != null) {
            allHallucinationFlags.addAll(llmDecision.getHallucinationFlags());
        }

        List<AuditDecisionDto.ViolationDto> allViolations = new ArrayList<>(compliance.violations());
        if (llmDecision.getComplianceViolations() != null) {
            allViolations.addAll(llmDecision.getComplianceViolations());
        }

        List<String> allToxicityFlags = new ArrayList<>(toxicity.flags());
        if (llmDecision.getToxicityFlags() != null) {
            allToxicityFlags.addAll(llmDecision.getToxicityFlags());
        }

        // Determine final verdict
        AuditVerdict finalVerdict = determineVerdict(
                consistency, hallucination, compliance, toxicity, llmDecision);

        // Calculate confidence
        double confidence = calculateConfidence(consistency, hallucination, compliance, llmDecision);

        // Build reasoning
        String reasoning = buildReasoning(consistency, hallucination, compliance, toxicity, llmDecision);

        return AuditDecisionDto.builder()
                .verdict(finalVerdict)
                .confidence(confidence)
                .consistencyScore(consistency.score())
                .hallucinationFlags(allHallucinationFlags)
                .complianceViolations(allViolations)
                .toxicityFlags(allToxicityFlags)
                .reasoning(reasoning)
                .build();
    }

    private AuditVerdict determineVerdict(
            ConsistencyChecker.ConsistencyResult consistency,
            HallucinationDetector.HallucinationResult hallucination,
            ComplianceScanner.ComplianceScanResult compliance,
            ToxicityFilter.ToxicityResult toxicity,
            AuditDecisionDto llmDecision) {

        boolean strictMode = config.getAudit().isStrictMode();

        // Hard BLOCK conditions
        if (compliance.maxSeverity() >= 1.0) return AuditVerdict.BLOCK;
        if (toxicity.maxSeverity() >= 0.9) return AuditVerdict.BLOCK;
        if (consistency.score() < 0.5) return AuditVerdict.BLOCK;
        if (hallucination.riskScore() >= 0.9) return AuditVerdict.BLOCK;
        if (llmDecision.getVerdict() == AuditVerdict.BLOCK) return AuditVerdict.BLOCK;

        // WARN conditions
        if (strictMode && (consistency.score() < 0.8 || hallucination.riskScore() > 0.3)) {
            return AuditVerdict.BLOCK;
        }

        if (compliance.maxSeverity() > 0.0) return AuditVerdict.WARN;
        if (toxicity.maxSeverity() > 0.0) return AuditVerdict.WARN;
        if (consistency.score() < 0.8) return AuditVerdict.WARN;
        if (hallucination.riskScore() > 0.3) return AuditVerdict.WARN;
        if (llmDecision.getVerdict() == AuditVerdict.WARN) return AuditVerdict.WARN;

        return AuditVerdict.PASS;
    }

    private double calculateConfidence(
            ConsistencyChecker.ConsistencyResult consistency,
            HallucinationDetector.HallucinationResult hallucination,
            ComplianceScanner.ComplianceScanResult compliance,
            AuditDecisionDto llmDecision) {

        double llmConfidence = llmDecision.getConfidence() != null ? llmDecision.getConfidence() : 0.7;

        // Weighted average of signals
        double score = (consistency.score() * 0.3) +
                ((1.0 - hallucination.riskScore()) * 0.2) +
                ((1.0 - compliance.maxSeverity()) * 0.2) +
                (llmConfidence * 0.3);

        return Math.round(score * 100.0) / 100.0;
    }

    private String buildReasoning(
            ConsistencyChecker.ConsistencyResult consistency,
            HallucinationDetector.HallucinationResult hallucination,
            ComplianceScanner.ComplianceScanResult compliance,
            ToxicityFilter.ToxicityResult toxicity,
            AuditDecisionDto llmDecision) {

        StringBuilder reasoning = new StringBuilder();

        reasoning.append(String.format("Consistency: %.0f%% agreement. ", consistency.score() * 100));

        if (!consistency.discrepancies().isEmpty()) {
            reasoning.append(String.format("%d discrepancies found. ", consistency.discrepancies().size()));
        }

        if (hallucination.riskScore() > 0) {
            reasoning.append(String.format("Hallucination risk: %.0f%%. ", hallucination.riskScore() * 100));
        }

        if (!compliance.violations().isEmpty()) {
            reasoning.append(String.format("%d compliance violations. ", compliance.violations().size()));
        }

        if (!toxicity.flags().isEmpty()) {
            reasoning.append(String.format("%d toxicity flags. ", toxicity.flags().size()));
        }

        if (llmDecision.getReasoning() != null && !llmDecision.getReasoning().isBlank()) {
            reasoning.append("LLM Assessment: ").append(llmDecision.getReasoning());
        }

        return reasoning.toString().trim();
    }
}
