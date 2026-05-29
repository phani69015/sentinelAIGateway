package com.sentinel.ai.model.dto;

import com.sentinel.ai.model.enums.ProviderType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponseDto {

    private ProviderType provider;
    private String model;
    private String text;
    private long latencyMs;
    private int inputTokens;
    private int outputTokens;
}
