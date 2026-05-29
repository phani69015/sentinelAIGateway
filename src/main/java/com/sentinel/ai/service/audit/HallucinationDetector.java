package com.sentinel.ai.service.audit;

import com.sentinel.ai.model.dto.LlmResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects potential hallucinations in LLM responses by identifying
 * claims that are not grounded in the provided context or that
 * contradict common financial knowledge.
 */
@Slf4j
@Service
public class HallucinationDetector {

    // Patterns indicating specific factual claims that need verification
    private static final Pattern SPECIFIC_RATE_PATTERN = Pattern.compile(
            "(currently|now|today|as of).*?(\\d+\\.?\\d*)\\s*(%|percent|APY|APR)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SPECIFIC_PRODUCT_PATTERN = Pattern.compile(
            "(our|we offer|the)\\s+([A-Z][\\w\\s]+)\\s+(account|fund|plan|product|card)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern REGULATORY_CLAIM_PATTERN = Pattern.compile(
            "(SEC|FINRA|FDIC|SIPC|IRS)\\s+(requires?|mandates?|states?|limits?)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Detect potential hallucinations by analyzing response content
     * against provided context and cross-referencing between responses.
     */
    public HallucinationResult detect(LlmResponseDto responseA, LlmResponseDto responseB, String context) {
        log.debug("Running hallucination detection");

        List<String> flags = new ArrayList<>();

        // Check for specific claims not grounded in context
        if (context != null && !context.isBlank()) {
            flags.addAll(checkUngroundedClaims(responseA.getText(), context, responseA.getProvider().name()));
            flags.addAll(checkUngroundedClaims(responseB.getText(), context, responseB.getProvider().name()));
        }

        // Check for overly specific claims that might be fabricated
        flags.addAll(checkFabricatedSpecifics(responseA.getText(), responseA.getProvider().name()));
        flags.addAll(checkFabricatedSpecifics(responseB.getText(), responseB.getProvider().name()));

        // Cross-reference: if one makes a very specific claim the other doesn't mention at all
        flags.addAll(crossReferenceSpecificClaims(responseA, responseB));

        double hallucinationRisk = calculateRisk(flags.size());

        log.info("Hallucination detection complete: flags={}, risk={}", flags.size(), hallucinationRisk);

        return new HallucinationResult(flags, hallucinationRisk);
    }

    private List<String> checkUngroundedClaims(String text, String context, String provider) {
        List<String> flags = new ArrayList<>();
        String lowerContext = context.toLowerCase();

        // Check specific rate claims against context
        Matcher rateMatcher = SPECIFIC_RATE_PATTERN.matcher(text);
        while (rateMatcher.find()) {
            String rateValue = rateMatcher.group(2);
            if (!lowerContext.contains(rateValue)) {
                flags.add(String.format(
                        "[%s] Claims rate of %s%s but this is not found in provided context",
                        provider, rateValue, rateMatcher.group(3)));
            }
        }

        // Check specific product name claims against context
        Matcher productMatcher = SPECIFIC_PRODUCT_PATTERN.matcher(text);
        while (productMatcher.find()) {
            String productName = productMatcher.group(2).trim().toLowerCase();
            if (!lowerContext.contains(productName)) {
                flags.add(String.format(
                        "[%s] References product '%s' which is not mentioned in provided context",
                        provider, productMatcher.group(2).trim()));
            }
        }

        return flags;
    }

    private List<String> checkFabricatedSpecifics(String text, String provider) {
        List<String> flags = new ArrayList<>();

        // Flag overly specific regulatory claims that could be fabricated
        Matcher regMatcher = REGULATORY_CLAIM_PATTERN.matcher(text);
        while (regMatcher.find()) {
            flags.add(String.format(
                    "[%s] Makes specific regulatory claim about %s — verify accuracy",
                    provider, regMatcher.group(0)));
        }

        return flags;
    }

    private List<String> crossReferenceSpecificClaims(LlmResponseDto responseA, LlmResponseDto responseB) {
        List<String> flags = new ArrayList<>();

        // Extract specific rates from each
        List<String> ratesA = extractRates(responseA.getText());
        List<String> ratesB = extractRates(responseB.getText());

        // If one mentions very specific rates that the other doesn't mention at all, flag it
        for (String rate : ratesA) {
            if (ratesB.isEmpty() || ratesB.stream().noneMatch(r -> r.equals(rate))) {
                // Only flag if response B doesn't mention any similar rate
                if (!ratesB.isEmpty()) {
                    flags.add(String.format(
                            "[%s] mentions rate '%s' that [%s] does not corroborate",
                            responseA.getProvider(), rate, responseB.getProvider()));
                }
            }
        }

        return flags;
    }

    private List<String> extractRates(String text) {
        List<String> rates = new ArrayList<>();
        Matcher matcher = Pattern.compile("(\\d+\\.?\\d*)\\s*(%|APY|APR)", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            rates.add(matcher.group(1) + matcher.group(2).toUpperCase());
        }
        return rates;
    }

    private double calculateRisk(int flagCount) {
        if (flagCount == 0) return 0.0;
        if (flagCount <= 2) return 0.3;
        if (flagCount <= 4) return 0.6;
        return 0.9;
    }

    public record HallucinationResult(List<String> flags, double riskScore) {}
}
