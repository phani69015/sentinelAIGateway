package com.sentinel.ai.service.orchestrator;

import com.sentinel.ai.model.dto.*;
import com.sentinel.ai.model.entity.AuditRecord;
import com.sentinel.ai.model.entity.ComplianceViolation;
import com.sentinel.ai.model.enums.AuditVerdict;
import com.sentinel.ai.model.enums.ProviderType;
import com.sentinel.ai.model.enums.ViolationType;
import com.sentinel.ai.repository.AuditRecordRepository;
import com.sentinel.ai.service.audit.AuditAgent;
import com.sentinel.ai.service.llm.LlmProvider;
import com.sentinel.ai.service.llm.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The main pipeline coordinator. Orchestrates the entire Sentinel AI flow:
 * 1. Parallel LLM calls to both providers
 * 2. Audit agent evaluation
 * 3. Verdict determination
 * 4. Audit trail persistence
 * 5. Response delivery
 */
@Slf4j
@Service
public class QueryOrchestrator {

    private static final String SAFE_FALLBACK_RESPONSE =
            "I'm unable to provide a verified response to your query at this time. " +
                    "Please contact a qualified financial advisor or visit your nearest branch " +
                    "for assistance. Reference ID: %s";

    private final ParallelLlmExecutor parallelExecutor;
    private final AuditAgent auditAgent;
    private final AuditRecordRepository auditRecordRepository;
    private final List<LlmProvider> llmProviders;

    public QueryOrchestrator(
            ParallelLlmExecutor parallelExecutor,
            AuditAgent auditAgent,
            AuditRecordRepository auditRecordRepository,
            List<LlmProvider> llmProviders) {
        this.parallelExecutor = parallelExecutor;
        this.auditAgent = auditAgent;
        this.auditRecordRepository = auditRecordRepository;
        this.llmProviders = llmProviders;
    }

    /**
     * Process a customer query through the full Sentinel pipeline.
     */
    public QueryResponse process(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing query: {}...", truncate(request.getQuery(), 50));

        // Step 1: Get the two responder providers
        LlmProvider providerA = getProvider(ProviderType.OPENAI);
        LlmProvider providerB = getProvider(ProviderType.ANTHROPIC);

        // Step 2: Build context string
        String contextStr = buildContextString(request.getContext());

        // Step 3: Build the system prompt with any provided context
        String systemPrompt = buildSystemPrompt(request.getContext());

        // Step 4: Execute parallel LLM calls
        ParallelLlmExecutor.ParallelResult parallelResult =
                parallelExecutor.executeParallel(providerA, providerB, systemPrompt, request.getQuery());

        LlmResponseDto responseA = parallelResult.responseA();
        LlmResponseDto responseB = parallelResult.responseB();

        // Step 5: Run audit agent
        AuditDecisionDto auditDecision = auditAgent.evaluate(
                responseA, responseB, request.getQuery(), contextStr);

        // Step 6: Determine which response to deliver
        String deliveredResponse = selectResponse(auditDecision, responseA, responseB);

        long latencyMs = System.currentTimeMillis() - startTime;

        // Step 7: Persist audit trail
        AuditRecord savedRecord = persistAuditRecord(
                request, responseA, responseB, auditDecision, deliveredResponse, latencyMs);

        // Step 8: Build and return response
        log.info("Query processed: verdict={}, confidence={}, latency={}ms",
                auditDecision.getVerdict(), auditDecision.getConfidence(), latencyMs);

        return buildResponse(savedRecord, auditDecision, deliveredResponse, latencyMs);
    }

    private LlmProvider getProvider(ProviderType type) {
        return llmProviders.stream()
                .filter(p -> p.getProviderType() == type)
                .filter(LlmProvider::isAvailable)
                .findFirst()
                .orElseThrow(() -> new com.sentinel.ai.exceptions.LlmProviderException(
                        "Provider " + type + " is not available. Check API key configuration."));
    }

    private String buildSystemPrompt(QueryRequest.QueryContext context) {
        if (context != null && context.getKnowledgeBase() != null && !context.getKnowledgeBase().isEmpty()) {
            return PromptTemplates.FINANCIAL_ADVISOR_SYSTEM_PROMPT +
                    "\n\n## Reference Information:\n" +
                    String.join("\n", context.getKnowledgeBase());
        }
        return PromptTemplates.FINANCIAL_ADVISOR_SYSTEM_PROMPT;
    }

    private String buildContextString(QueryRequest.QueryContext context) {
        if (context == null) return null;
        StringBuilder sb = new StringBuilder();
        if (context.getCustomerSegment() != null) {
            sb.append("Customer Segment: ").append(context.getCustomerSegment()).append("\n");
        }
        if (context.getProductCategory() != null) {
            sb.append("Product Category: ").append(context.getProductCategory()).append("\n");
        }
        if (context.getKnowledgeBase() != null) {
            sb.append("Knowledge Base:\n").append(String.join("\n", context.getKnowledgeBase()));
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String selectResponse(AuditDecisionDto decision, LlmResponseDto responseA, LlmResponseDto responseB) {
        return switch (decision.getVerdict()) {
            case PASS -> {
                // Return the response with higher consistency (prefer A by default)
                yield responseA.getText();
            }
            case WARN -> {
                // Return response but flag for review
                yield responseA.getText() +
                        "\n\n---\n*Note: This response has been flagged for human review. " +
                        "Please verify the information with a qualified advisor.*";
            }
            case BLOCK -> null; // Will use safe fallback
        };
    }

    private AuditRecord persistAuditRecord(
            QueryRequest request,
            LlmResponseDto responseA,
            LlmResponseDto responseB,
            AuditDecisionDto auditDecision,
            String deliveredResponse,
            long latencyMs) {

        List<ComplianceViolation> violationEntities = new ArrayList<>();

        AuditRecord record = AuditRecord.builder()
                .query(request.getQuery())
                .contextData(buildContextString(request.getContext()))
                .responseA(responseA.getText())
                .responseAProvider(responseA.getProvider())
                .responseAModel(responseA.getModel())
                .responseB(responseB.getText())
                .responseBProvider(responseB.getProvider())
                .responseBModel(responseB.getModel())
                .verdict(auditDecision.getVerdict())
                .confidenceScore(auditDecision.getConfidence())
                .consistencyScore(auditDecision.getConsistencyScore() != null ?
                        auditDecision.getConsistencyScore() : 1.0)
                .auditReasoning(auditDecision.getReasoning())
                .deliveredResponse(deliveredResponse)
                .latencyMs(latencyMs)
                .violations(violationEntities)
                .build();

        // Map violation DTOs to entities
        if (auditDecision.getComplianceViolations() != null) {
            for (AuditDecisionDto.ViolationDto dto : auditDecision.getComplianceViolations()) {
                violationEntities.add(ComplianceViolation.builder()
                        .auditRecord(record)
                        .violationType(dto.getType())
                        .description(dto.getDescription())
                        .severity(dto.getSeverity())
                        .offendingText(dto.getOffendingText())
                        .sourceProvider(dto.getSourceProvider() != null ?
                                ProviderType.valueOf(dto.getSourceProvider()) : null)
                        .build());
            }
        }

        return auditRecordRepository.save(record);
    }

    private QueryResponse buildResponse(
            AuditRecord record,
            AuditDecisionDto auditDecision,
            String deliveredResponse,
            long latencyMs) {

        String finalResponse = deliveredResponse != null ?
                deliveredResponse :
                String.format(SAFE_FALLBACK_RESPONSE, record.getId());

        int violationsFound = auditDecision.getComplianceViolations() != null ?
                auditDecision.getComplianceViolations().size() : 0;

        return QueryResponse.builder()
                .response(finalResponse)
                .auditId(record.getId())
                .verdict(auditDecision.getVerdict())
                .confidence(auditDecision.getConfidence())
                .metadata(QueryResponse.ResponseMetadata.builder()
                        .respondersAgreed(auditDecision.getConsistencyScore() != null &&
                                auditDecision.getConsistencyScore() > 0.8)
                        .complianceChecks(6) // Number of rule categories
                        .violationsFound(violationsFound)
                        .latencyMs(latencyMs)
                        .build())
                .build();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
