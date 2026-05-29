package com.sentinel.ai.service.orchestrator;

import com.sentinel.ai.exceptions.LlmProviderException;
import com.sentinel.ai.model.dto.LlmResponseDto;
import com.sentinel.ai.service.llm.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.StructuredTaskScope;

/**
 * Executes LLM calls in parallel using Java 21+ virtual threads
 * with StructuredTaskScope for clean lifecycle management.
 */
@Slf4j
@Service
public class ParallelLlmExecutor {

    public record ParallelResult(LlmResponseDto responseA, LlmResponseDto responseB) {}

    /**
     * Execute two LLM providers in parallel and return both responses.
     * Uses StructuredTaskScope to ensure both tasks complete or fail together.
     */
    public ParallelResult executeParallel(
            LlmProvider providerA,
            LlmProvider providerB,
            String systemPrompt,
            String userPrompt) {

        log.info("Executing parallel LLM calls: providerA={}, providerB={}",
                providerA.getProviderType(), providerB.getProviderType());

        try (var scope = StructuredTaskScope.open()) {

            var taskA = scope.fork(() -> {
                log.debug("Starting LLM call to {}", providerA.getProviderType());
                return providerA.complete(systemPrompt, userPrompt);
            });

            var taskB = scope.fork(() -> {
                log.debug("Starting LLM call to {}", providerB.getProviderType());
                return providerB.complete(systemPrompt, userPrompt);
            });

            scope.join();

            LlmResponseDto resultA = taskA.get();
            LlmResponseDto resultB = taskB.get();

            log.info("Both LLM calls completed: A={}ms, B={}ms",
                    resultA.getLatencyMs(), resultB.getLatencyMs());

            return new ParallelResult(resultA, resultB);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmProviderException("Parallel LLM execution interrupted", e);
        } catch (Exception e) {
            log.error("LLM execution failed", e);
            throw new LlmProviderException("Parallel LLM execution error: " + e.getMessage(), e);
        }
    }
}
