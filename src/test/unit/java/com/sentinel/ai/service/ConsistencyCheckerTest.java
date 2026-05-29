package com.sentinel.ai.service;

import com.sentinel.ai.model.dto.LlmResponseDto;
import com.sentinel.ai.model.enums.ProviderType;
import com.sentinel.ai.service.audit.ConsistencyChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsistencyCheckerTest {

    private ConsistencyChecker consistencyChecker;

    @BeforeEach
    void setUp() {
        consistencyChecker = new ConsistencyChecker();
    }

    @Test
    @DisplayName("Should return high consistency score when responses agree")
    void testConsistentResponses() {
        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("The savings account offers 4.25% APY with a $10,000 minimum balance. " +
                        "It is FDIC insured up to $250,000.")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("This savings account provides a 4.25% APY. You need a minimum balance of $10,000. " +
                        "The account is FDIC insured for up to $250,000.")
                .build();

        var result = consistencyChecker.check(responseA, responseB);

        assertTrue(result.score() >= 0.8, "Consistent responses should score >= 0.8");
        assertTrue(result.discrepancies().isEmpty(), "Should have no discrepancies");
    }

    @Test
    @DisplayName("Should detect numerical discrepancy in rates")
    void testInconsistentRates() {
        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("The savings account offers 4.25% APY on all balances.")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("The savings account offers 3.75% APY on all balances.")
                .build();

        var result = consistencyChecker.check(responseA, responseB);

        assertTrue(result.score() < 1.0, "Inconsistent rates should lower the score");
        assertFalse(result.discrepancies().isEmpty(), "Should detect the rate discrepancy");
    }

    @Test
    @DisplayName("Should detect contradictory statements")
    void testContradictoryStatements() {
        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("This account has no fee for transactions and is a great option.")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("Please note that a fee applies for each transaction made from this account.")
                .build();

        var result = consistencyChecker.check(responseA, responseB);

        assertFalse(result.discrepancies().isEmpty(), "Should detect contradiction about fees");
    }

    @Test
    @DisplayName("Should handle empty responses gracefully")
    void testEmptyResponses() {
        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("")
                .build();

        var result = consistencyChecker.check(responseA, responseB);

        assertNotNull(result);
        assertEquals(1.0, result.score(), "Empty responses should be consistent (nothing to disagree on)");
    }

    @Test
    @DisplayName("Should allow small rounding differences in values")
    void testSmallRoundingDifferences() {
        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("The interest rate is approximately 4.24% APY.")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("The interest rate is about 4.25% APY.")
                .build();

        var result = consistencyChecker.check(responseA, responseB);

        // Within 5% tolerance, these should not be flagged
        assertTrue(result.discrepancies().isEmpty(),
                "Values within 5% tolerance should not be flagged");
    }
}
