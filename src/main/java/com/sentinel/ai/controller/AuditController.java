package com.sentinel.ai.controller;

import com.sentinel.ai.model.dto.AuditDecisionDto;
import com.sentinel.ai.model.dto.AuditRecordDto;
import com.sentinel.ai.model.entity.AuditRecord;
import com.sentinel.ai.model.enums.AuditVerdict;
import com.sentinel.ai.repository.AuditRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditRecordRepository auditRecordRepository;

    public AuditController(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    /**
     * Retrieve a specific audit record by ID.
     * Used for compliance review and regulatory reporting.
     */
    @GetMapping("/{auditId}")
    public ResponseEntity<AuditRecordDto> getAuditRecord(@PathVariable UUID auditId) {
        log.info("Retrieving audit record: {}", auditId);

        return auditRecordRepository.findById(auditId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List audit records with optional filtering by verdict.
     */
    @GetMapping
    public ResponseEntity<Page<AuditRecordDto>> listAuditRecords(
            @RequestParam(required = false) AuditVerdict verdict,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Listing audit records: verdict={}, page={}, size={}", verdict, page, size);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AuditRecord> records;
        if (verdict != null) {
            records = auditRecordRepository.findByVerdictOrderByCreatedAtDesc(verdict, pageRequest);
        } else {
            records = auditRecordRepository.findAll(pageRequest);
        }

        return ResponseEntity.ok(records.map(this::toDto));
    }

    /**
     * Get summary statistics for audit records.
     */
    @GetMapping("/stats")
    public ResponseEntity<AuditStats> getStats() {
        long total = auditRecordRepository.count();
        long passed = auditRecordRepository.countByVerdict(AuditVerdict.PASS);
        long warned = auditRecordRepository.countByVerdict(AuditVerdict.WARN);
        long blocked = auditRecordRepository.countByVerdict(AuditVerdict.BLOCK);

        return ResponseEntity.ok(new AuditStats(total, passed, warned, blocked));
    }

    private AuditRecordDto toDto(AuditRecord record) {
        return AuditRecordDto.builder()
                .id(record.getId())
                .timestamp(record.getCreatedAt())
                .query(record.getQuery())
                .responseA(AuditRecordDto.LlmResponseDto.builder()
                        .provider(record.getResponseAProvider())
                        .text(record.getResponseA())
                        .model(record.getResponseAModel())
                        .build())
                .responseB(AuditRecordDto.LlmResponseDto.builder()
                        .provider(record.getResponseBProvider())
                        .text(record.getResponseB())
                        .model(record.getResponseBModel())
                        .build())
                .auditDecision(AuditDecisionDto.builder()
                        .verdict(record.getVerdict())
                        .confidence(record.getConfidenceScore())
                        .consistencyScore(record.getConsistencyScore())
                        .reasoning(record.getAuditReasoning())
                        .build())
                .deliveredResponse(record.getDeliveredResponse())
                .verdict(record.getVerdict())
                .latencyMs(record.getLatencyMs())
                .build();
    }

    public record AuditStats(long total, long passed, long warned, long blocked) {}
}
