package com.sentinel.ai.util;

import java.util.UUID;

/**
 * Utility for generating audit IDs.
 * Using UUID v7 would be ideal for time-ordered IDs,
 * but UUID v4 via JPA is sufficient for this implementation.
 */
public final class AuditIdGenerator {

    private AuditIdGenerator() {}

    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
