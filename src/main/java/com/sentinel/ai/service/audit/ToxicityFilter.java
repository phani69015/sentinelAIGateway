package com.sentinel.ai.service.audit;

import com.sentinel.ai.model.dto.LlmResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filters LLM responses for toxic, discriminatory, or inappropriate content
 * that would violate fair lending rules or be unsuitable for customer-facing use.
 */
@Slf4j
@Service
public class ToxicityFilter {

    private static final List<ToxicityPattern> TOXICITY_PATTERNS = List.of(
            new ToxicityPattern(
                    "DISCRIMINATORY_LANGUAGE",
                    Pattern.compile(
                            "(because of your|based on your|due to your)\\s+(race|gender|sex|age|religion|ethnicity|nationality|disability|marital status|sexual orientation)",
                            Pattern.CASE_INSENSITIVE),
                    1.0),
            new ToxicityPattern(
                    "STEREOTYPING",
                    Pattern.compile(
                            "(people like you|your kind|your type|your demographic)\\s+(usually|typically|often|tend to|always)",
                            Pattern.CASE_INSENSITIVE),
                    0.9),
            new ToxicityPattern(
                    "AGGRESSIVE_SALES",
                    Pattern.compile(
                            "(you must act now|limited time only|you('ll| will) miss out|don't be a fool|you'd be stupid)",
                            Pattern.CASE_INSENSITIVE),
                    0.6),
            new ToxicityPattern(
                    "FEAR_MONGERING",
                    Pattern.compile(
                            "(you will lose everything|financial ruin|go bankrupt|lose your home)\\s+(if you don't|unless you)",
                            Pattern.CASE_INSENSITIVE),
                    0.7),
            new ToxicityPattern(
                    "PROFANITY",
                    Pattern.compile(
                            "\\b(damn|hell|shit|fuck|ass|bastard|crap)\\b",
                            Pattern.CASE_INSENSITIVE),
                    0.8),
            new ToxicityPattern(
                    "CONDESCENSION",
                    Pattern.compile(
                            "(obviously you don't understand|clearly you can't|someone like you wouldn't|that's a stupid question)",
                            Pattern.CASE_INSENSITIVE),
                    0.7)
    );

    /**
     * Filter both responses for toxic content.
     */
    public ToxicityResult filter(LlmResponseDto responseA, LlmResponseDto responseB) {
        log.debug("Running toxicity filter");

        List<String> flags = new ArrayList<>();

        flags.addAll(scanForToxicity(responseA.getText(), responseA.getProvider().name()));
        flags.addAll(scanForToxicity(responseB.getText(), responseB.getProvider().name()));

        double maxSeverity = flags.isEmpty() ? 0.0 :
                TOXICITY_PATTERNS.stream()
                        .filter(p -> flags.stream().anyMatch(f -> f.contains(p.name())))
                        .mapToDouble(ToxicityPattern::severity)
                        .max()
                        .orElse(0.0);

        log.info("Toxicity filter complete: flags={}, maxSeverity={}", flags.size(), maxSeverity);

        return new ToxicityResult(flags, maxSeverity);
    }

    private List<String> scanForToxicity(String text, String provider) {
        List<String> flags = new ArrayList<>();

        for (ToxicityPattern tp : TOXICITY_PATTERNS) {
            var matcher = tp.pattern().matcher(text);
            if (matcher.find()) {
                flags.add(String.format("[%s] %s: '%s'", provider, tp.name(), matcher.group()));
            }
        }

        return flags;
    }

    public record ToxicityResult(List<String> flags, double maxSeverity) {}

    private record ToxicityPattern(String name, Pattern pattern, double severity) {}
}
