package com.sentinel.ai.service.llm;

import com.sentinel.ai.model.dto.LlmResponseDto;
import com.sentinel.ai.model.enums.ProviderType;

/**
 * Interface for LLM providers. Each provider must implement this contract
 * to participate in the Sentinel AI pipeline.
 */
public interface LlmProvider {

    /**
     * Send a completion request to the LLM provider.
     *
     * @param systemPrompt The system-level instructions for the model
     * @param userPrompt   The user query/prompt
     * @return LlmResponseDto containing the model's response and metadata
     */
    LlmResponseDto complete(String systemPrompt, String userPrompt);

    /**
     * Get the provider type identifier.
     */
    ProviderType getProviderType();

    /**
     * Check if this provider is properly configured and ready to use.
     */
    boolean isAvailable();
}
