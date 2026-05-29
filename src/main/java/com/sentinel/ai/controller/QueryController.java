package com.sentinel.ai.controller;

import com.sentinel.ai.model.dto.QueryRequest;
import com.sentinel.ai.model.dto.QueryResponse;
import com.sentinel.ai.service.orchestrator.QueryOrchestrator;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class QueryController {

    private final QueryOrchestrator queryOrchestrator;

    public QueryController(QueryOrchestrator queryOrchestrator) {
        this.queryOrchestrator = queryOrchestrator;
    }

    /**
     * Process a customer query through the Sentinel AI pipeline.
     * Runs two independent LLMs in parallel, cross-validates with the Audit Agent,
     * and returns a verified response with audit metadata.
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> processQuery(@Valid @RequestBody QueryRequest request) {
        log.info("Received query request");

        QueryResponse response = queryOrchestrator.process(request);

        return ResponseEntity.ok()
                .header("X-Audit-Id", response.getAuditId().toString())
                .header("X-Verdict", response.getVerdict().name())
                .body(response);
    }
}
