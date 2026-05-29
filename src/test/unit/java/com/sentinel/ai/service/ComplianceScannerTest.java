package com.sentinel.ai.service;

import com.sentinel.ai.model.dto.LlmResponseDto;
import com.sentinel.ai.model.enums.ProviderType;
import com.sentinel.ai.service.audit.ToxicityFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToxicityFilterTest {

    private ToxicityFilter toxicityFilter;

    @BeforeEach
    void setUp() {
        toxicityFilter = new ToxicityFilter();
    }

    @Test
    @DisplayName("Should pass clean responses")
    void testCleanResponses() {
        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("A savings account is a great way to build an emergency fund. " +
                        "Consider consulting a financial advisor for personalized recommendations.")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("Savings accounts offer a safe place to store money while earning interest. " +
                        "This information is general in nature.")
                .build();

        var result = toxicityFilter.filter(responseA, responseB);

        assertTrue(result.flags().isEmpty(), "Clean responses should have no toxicity flags");
        assertEquals(0.0, result.maxSeverity());
    }

    @Test
    @DisplayName("Should detect discriminatory language")
    void testDiscriminatoryLanguage() {
        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("Based on your age and gender, I would recommend a more conservative portfolio. " +
                        "Because of your ethnicity, you may face different approval rates.")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("I recommend diversified investments regardless of personal characteristics.")
                .build();

        var result = toxicityFilter.filter(responseA, responseB);

        assertFalse(result.flags().isEmpty(), "Should detect discriminatory content");
        assertTrue(result.maxSeverity() >= 0.9, "Discriminatory content should have high severity");
    }

    @Test
    @DisplayName("Should detect aggressive sales tactics")
    void testAggressiveSales() {
        LlmResponseDto responseA = LlmResponseDto.builder()
                .provider(ProviderType.OPENAI)
                .text("You must act now or you'll miss out on this incredible opportunity! " +
                        "Don't be a fool - this offer expires today!")
                .build();

        LlmResponseDto responseB = LlmResponseDto.builder()
                .provider(ProviderType.ANTHROPIC)
                .text("Take your time to research options. There's no rush to make financial decisions.")
                .build();

        var result = toxicityFilter.filter(responseA, responseB);

        assertFalse(result.flags().isEmpty(), "Should detect aggressive sales tactics");
    }
}
