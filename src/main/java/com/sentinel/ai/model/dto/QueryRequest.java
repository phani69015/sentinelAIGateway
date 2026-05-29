package com.sentinel.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    @NotBlank(message = "Query must not be blank")
    @Size(max = 10000, message = "Query must not exceed 10000 characters")
    private String query;

    private QueryContext context;

    private QueryOptions options;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryContext {
        private String customerSegment;
        private String productCategory;
        private List<String> knowledgeBase;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryOptions {
        @Builder.Default
        private boolean strictMode = false;

        @Builder.Default
        private long timeoutMs = 30000;
    }
}
