package com.sentinel.ai.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sentinel.ai.config.LlmProviderConfig;
import com.sentinel.ai.exceptions.LlmProviderException;
import com.sentinel.ai.model.dto.LlmResponseDto;
import com.sentinel.ai.model.enums.ProviderType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
public class AnthropicProvider implements LlmProvider {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final LlmProviderConfig.ProviderSettings settings;

    public AnthropicProvider(HttpClient httpClient, ObjectMapper objectMapper, LlmProviderConfig config) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.settings = config.getProviders().getAnthropic();
    }

    @Override
    public LlmResponseDto complete(String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();

        try {
            String requestBody = buildRequestBody(systemPrompt, userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(settings.getBaseUrl() + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", settings.getApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Anthropic API error: status={}, body={}", response.statusCode(), response.body());
                throw new LlmProviderException("Anthropic API returned status " + response.statusCode());
            }

            return parseResponse(response.body(), startTime);

        } catch (LlmProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Anthropic API", e);
            throw new LlmProviderException("Failed to call Anthropic API: " + e.getMessage(), e);
        }
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.ANTHROPIC;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.isNotBlank(settings.getApiKey());
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", settings.getModel());
            root.put("max_tokens", settings.getMaxTokens());
            root.put("temperature", settings.getTemperature());
            root.put("system", systemPrompt);

            ArrayNode messages = root.putArray("messages");

            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new LlmProviderException("Failed to build request body", e);
        }
    }

    private LlmResponseDto parseResponse(String responseBody, long startTime) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.get("content");

            if (content == null || content.isEmpty()) {
                throw new LlmProviderException("No content in Anthropic response");
            }

            // Anthropic returns content as array of blocks
            StringBuilder textBuilder = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.get("type").asText())) {
                    textBuilder.append(block.get("text").asText());
                }
            }

            String model = root.get("model").asText();

            JsonNode usage = root.get("usage");
            int inputTokens = usage != null ? usage.get("input_tokens").asInt() : 0;
            int outputTokens = usage != null ? usage.get("output_tokens").asInt() : 0;

            return LlmResponseDto.builder()
                    .provider(ProviderType.ANTHROPIC)
                    .model(model)
                    .text(textBuilder.toString())
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .build();

        } catch (LlmProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmProviderException("Failed to parse Anthropic response", e);
        }
    }
}
