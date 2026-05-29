package com.sentinel.ai.service.audit;

import com.sentinel.ai.model.dto.AuditDecisionDto;
import com.sentinel.ai.model.dto.LlmResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks consistency between two LLM responses.
 * Identifies factual discrepancies by comparing numerical claims,
 * named entities, and key statements.
 */
@Slf4j
@Service
public class ConsistencyChecker {

    // Pattern to find numerical claims (percentages, dollar amounts, dates)
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "(\\d+\\.?\\d*)\\s*(%|percent|dollars?|\\$|years?|months?|days?|APY|APR|bps|basis points)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Compare two LLM responses for factual consistency.
     *
     * @return ConsistencyResult with score (0-1) and list of discrepancies
     */
    public ConsistencyResult check(LlmResponseDto responseA, LlmResponseDto responseB) {
        log.debug("Running consistency check between {} and {}",
                responseA.getProvider(), responseB.getProvider());

        List<String> discrepancies = new ArrayList<>();

        // Extract and compare numerical claims
        List<NumericalClaim> claimsA = extractNumericalClaims(responseA.getText());
        List<NumericalClaim> claimsB = extractNumericalClaims(responseB.getText());

        for (NumericalClaim claimA : claimsA) {
            for (NumericalClaim claimB : claimsB) {
                if (claimA.unit().equalsIgnoreCase(claimB.unit()) &&
                        !valuesMatch(claimA.value(), claimB.value())) {
                    discrepancies.add(String.format(
                            "Numerical discrepancy: %s says %s %s, %s says %s %s",
                            responseA.getProvider(), claimA.value(), claimA.unit(),
                            responseB.getProvider(), claimB.value(), claimB.unit()));
                }
            }
        }

        // Check for contradictory statements
        List<String> contradictions = findContradictions(responseA.getText(), responseB.getText());
        discrepancies.addAll(contradictions);

        // Calculate consistency score
        double score = calculateScore(discrepancies.size(), responseA.getText(), responseB.getText());

        log.info("Consistency check complete: score={}, discrepancies={}", score, discrepancies.size());

        return new ConsistencyResult(score, discrepancies);
    }

    private List<NumericalClaim> extractNumericalClaims(String text) {
        List<NumericalClaim> claims = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            claims.add(new NumericalClaim(
                    Double.parseDouble(matcher.group(1)),
                    matcher.group(2).toLowerCase()
            ));
        }
        return claims;
    }

    private boolean valuesMatch(double a, double b) {
        // Allow 5% tolerance for rounding differences
        if (a == 0 && b == 0) return true;
        double diff = Math.abs(a - b);
        double avg = (Math.abs(a) + Math.abs(b)) / 2.0;
        return (diff / avg) <= 0.05;
    }

    private List<String> findContradictions(String textA, String textB) {
        List<String> contradictions = new ArrayList<>();

        // Check for direct negation patterns
        String[][] opposites = {
                {"no fee", "fee applies"},
                {"no minimum", "minimum balance"},
                {"fdic insured", "not fdic insured"},
                {"no penalty", "early withdrawal penalty"},
                {"fixed rate", "variable rate"},
                {"guaranteed", "not guaranteed"}
        };

        String lowerA = textA.toLowerCase();
        String lowerB = textB.toLowerCase();

        for (String[] pair : opposites) {
            if ((lowerA.contains(pair[0]) && lowerB.contains(pair[1])) ||
                    (lowerA.contains(pair[1]) && lowerB.contains(pair[0]))) {
                contradictions.add(String.format(
                        "Contradictory claim detected: one says '%s', other says '%s'",
                        pair[0], pair[1]));
            }
        }

        return contradictions;
    }

    private double calculateScore(int discrepancyCount, String textA, String textB) {
        if (discrepancyCount == 0) return 1.0;

        // More discrepancies = lower score, weighted by text length
        int avgLength = (textA.length() + textB.length()) / 2;
        double normalizedDiscrepancies = (double) discrepancyCount / Math.max(1, avgLength / 100);

        return Math.max(0.0, 1.0 - (normalizedDiscrepancies * 0.3));
    }

    public record ConsistencyResult(double score, List<String> discrepancies) {}

    private record NumericalClaim(double value, String unit) {}
}
