package com.sentinel.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.ai.model.dto.QueryRequest;
import com.sentinel.ai.model.dto.QueryResponse;
import com.sentinel.ai.model.enums.AuditVerdict;
import com.sentinel.ai.service.orchestrator.QueryOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueryOrchestrator queryOrchestrator;

    @Test
    @DisplayName("Should return 200 with valid query")
    void testValidQuery() throws Exception {
        UUID auditId = UUID.randomUUID();
        QueryResponse mockResponse = QueryResponse.builder()
                .response("A savings account earns interest on deposits.")
                .auditId(auditId)
                .verdict(AuditVerdict.PASS)
                .confidence(0.95)
                .metadata(QueryResponse.ResponseMetadata.builder()
                        .respondersAgreed(true)
                        .complianceChecks(6)
                        .violationsFound(0)
                        .latencyMs(2500)
                        .build())
                .build();

        when(queryOrchestrator.process(any(QueryRequest.class))).thenReturn(mockResponse);

        QueryRequest request = QueryRequest.builder()
                .query("What is a savings account?")
                .build();

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PASS"))
                .andExpect(jsonPath("$.auditId").value(auditId.toString()))
                .andExpect(jsonPath("$.confidence").value(0.95))
                .andExpect(header().string("X-Audit-Id", auditId.toString()))
                .andExpect(header().string("X-Verdict", "PASS"));
    }

    @Test
    @DisplayName("Should return 400 for blank query")
    void testBlankQuery() throws Exception {
        QueryRequest request = QueryRequest.builder()
                .query("")
                .build();

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for missing query field")
    void testMissingQuery() throws Exception {
        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
