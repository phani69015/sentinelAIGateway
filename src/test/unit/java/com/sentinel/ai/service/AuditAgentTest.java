package com.sentinel.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.ai.config.LlmProviderConfig;
import com.sentinel.ai.model.dto.AuditDecisionDto;
import com.sentinel.ai.model.dto.LlmResponseDto;
import com.sentinel.ai.model.enums.AuditVerdict;
import com.sentinel.ai.model.enums.ProviderType;
import com.sentinel.ai.service.audit.*;
import com.sentinel.ai.service.llm.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAgentTest {

    @Mock
    private LlmProvider mockAuditProvider;

    private AuditAgent auditAgent;
    private ConsistencyChecker consistencyChecker;
    private HallucinationDetector hallucinationDetector;
    private ComplianceScanner complianceScanner;
    private ToxicityFilter toxicityFilter;
    private LlmProviderConfig config;

    @BeforeEach
    void setUp() {
        consistencyChecker = new ConsistencyChecker();
        hallucinationDetector = new HallucinationDetector();
        toxicityFilter = new ToxicityFilter();

        config = new LlmProviderConfig();
        config.setAudit(new LlmProviderConfig.Audit());
        config.getAudit().setProvider(ProviderType.ANTHROPIC);
        config.getAudit().setStrictMode(false);
        config.setCompliance(new LlmProviderConfig.Compliance());
        config.getCompliance().setRulesPath("classpath:rules/sec-finra-rules.json");

        // ComplianceScanner needs ResourceLoader, we'll test it separately
        complianceScanner = new ComplianceScanner(new ObjectMapper(), null, config);

        when(mockAuditProvider.getProviderType()).thenReturn(ProviderType.ANTHROPIC);
        when(mockAuditProvider.isAvailable()).thenReturn(true);

        auditAgent = new AuditAgent(
                consistencyChecker,
                hallucinationDetector,
                complianceScanner,
                toxicityFilter,
                List.of(mockAuditProvider),
                config,
                new ObjectMapper());
    }

    @Test
    @DisplayName("Should PASS when both responses are consistent and compliant")
    void testPassScenario() {
        // Mock the audit LLM to return PASS
        String auditJson = """
                {
                    "verdict": "PASS",
                    "confidence": 0.95,
                    "consistencyScore": 0.98,
                    "hallucinationFlags": [],
                    "complianceViolations": [],
                    "toxicityFlags": [],
                    "reasoning": "Both responses provide consistent, accurate information.",
                    "recommendedResponse": "RESPONSE_A"
                }
                """;

        when(mockAuditProvider.complete(anyString(), anyString()))
                .thenReturn(LlmResponseDto.builder()
                        .provider(ProviderType.ANTHROPIC)
                        .text(auditJson)
                        .build());

        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("A savings account is a bank account that earns interest on deposits. " +
                        "It differs from checking in that it's designed for saving rather than daily transactions. " +
                        "Please consult a financial advisor for personalized advice.")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("A savings account earns interest on your deposited money. " +
                        "Unlike checking accounts, savings accounts are meant for accumulating funds. " +
                        "This is general information only and not personalized advice.")
                .build();

        AuditDecisionDto result = auditAgent.evaluate(responseA, responseB,
                "What is a savings account?", null);

        assertEquals(AuditVerdict.PASS, result.getVerdict());
        assertTrue(result.getConfidence() > 0.7);
    }

    @Test
    @DisplayName("Should BLOCK when response contains guaranteed returns")
    void testBlockGuaranteedReturns() {
        String auditJson = """
                {
                    "verdict": "BLOCK",
                    "confidence": 0.99,
                    "consistencyScore": 0.5,
                    "hallucinationFlags": ["Fabricated guaranteed return claim"],
                    "complianceViolations": [{
                        "type": "GUARANTEED_RETURNS",
                        "description": "Response promises guaranteed 10% return",
                        "severity": 1.0,
                        "offendingText": "will earn 10% guaranteed",
                        "sourceProvider": "OPENAI"
                    }],
                    "toxicityFlags": [],
                    "reasoning": "Hard compliance violation: guaranteed returns claim.",
                    "recommendedResponse": "NEITHER"
                }
                """;

        when(mockAuditProvider.complete(anyString(), anyString()))
                .thenReturn(LlmResponseDto.builder()
                        .provider(ProviderType.ANTHROPIC)
                        .text(auditJson)
                        .build());

        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("You will earn 10% guaranteed annually with our premium investment fund. " +
                        "There is no risk involved and your returns are assured.")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("No investment can guarantee specific returns. Past performance does not " +
                        "indicate future results. Please consult a financial advisor.")
                .build();

        AuditDecisionDto result = auditAgent.evaluate(responseA, responseB,
                "Guarantee me 10% returns", null);

        assertEquals(AuditVerdict.BLOCK, result.getVerdict());
    }

    @Test
    @DisplayName("Should handle LLM audit failure gracefully")
    void testLlmAuditFailure() {
        when(mockAuditProvider.complete(anyString(), anyString()))
                .thenThrow(new RuntimeException("API timeout"));

        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("A savings account earns interest. Consult an advisor for personal guidance.")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("A savings account pays interest on deposits. This is general information only.")
                .build();

        // Should not throw, should fall back gracefully
        AuditDecisionDto result = auditAgent.evaluate(responseA, responseB,
                "What is a savings account?", null);

        assertNotNull(result);
        assertNotNull(result.getVerdict());
    }
}
