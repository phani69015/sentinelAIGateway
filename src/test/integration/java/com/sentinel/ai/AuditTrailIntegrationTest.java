package com.sentinel.ai;

import com.sentinel.ai.model.entity.AuditRecord;
import com.sentinel.ai.model.enums.AuditVerdict;
import com.sentinel.ai.model.enums.ProviderType;
import com.sentinel.ai.repository.AuditRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AuditTrailIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sentinel_test")
            .withUsername("sentinel")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @Test
    @DisplayName("Should persist and retrieve audit records")
    void testAuditRecordPersistence() {
        AuditRecord record = AuditRecord.builder()
                .query("What is a savings account?")
                .responseA("A savings account earns interest.")
                .responseAProvider(ProviderType.OPENAI)
                .responseAModel("gpt-4o")
                .responseB("Savings accounts pay interest on deposits.")
                .responseBProvider(ProviderType.ANTHROPIC)
                .responseBModel("claude-sonnet-4-20250514")
                .verdict(AuditVerdict.PASS)
                .confidenceScore(0.95)
                .consistencyScore(0.98)
                .auditReasoning("Both responses are consistent and compliant.")
                .deliveredResponse("A savings account earns interest.")
                .latencyMs(2500L)
                .build();

        AuditRecord saved = auditRecordRepository.save(record);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<AuditRecord> retrieved = auditRecordRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(AuditVerdict.PASS, retrieved.get().getVerdict());
        assertEquals("What is a savings account?", retrieved.get().getQuery());
    }

    @Test
    @DisplayName("Should count records by verdict")
    void testVerdictCounting() {
        // Create records with different verdicts
        auditRecordRepository.save(buildRecord(AuditVerdict.PASS));
        auditRecordRepository.save(buildRecord(AuditVerdict.PASS));
        auditRecordRepository.save(buildRecord(AuditVerdict.WARN));
        auditRecordRepository.save(buildRecord(AuditVerdict.BLOCK));

        long passCount = auditRecordRepository.countByVerdict(AuditVerdict.PASS);
        long warnCount = auditRecordRepository.countByVerdict(AuditVerdict.WARN);
        long blockCount = auditRecordRepository.countByVerdict(AuditVerdict.BLOCK);

        assertTrue(passCount >= 2);
        assertTrue(warnCount >= 1);
        assertTrue(blockCount >= 1);
    }

    private AuditRecord buildRecord(AuditVerdict verdict) {
        return AuditRecord.builder()
                .query("Test query")
                .responseA("Response A text")
                .responseAProvider(ProviderType.OPENAI)
                .responseAModel("gpt-4o")
                .responseB("Response B text")
                .responseBProvider(ProviderType.ANTHROPIC)
                .responseBModel("claude-sonnet-4-20250514")
                .verdict(verdict)
                .confidenceScore(0.9)
                .consistencyScore(0.9)
                .auditReasoning("Test reasoning")
                .deliveredResponse("Test delivered response")
                .latencyMs(1000L)
                .build();
    }
}
