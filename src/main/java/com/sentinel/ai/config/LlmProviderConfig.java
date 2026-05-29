package com.sentinel.ai.config;

import com.sentinel.ai.model.enums.ProviderType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sentinel")
@Getter
@Setter
public class LlmProviderConfig {

    private Providers providers = new Providers();
    private Audit audit = new Audit();
    private Compliance compliance = new Compliance();

    @Getter
    @Setter
    public static class Providers {
        private ProviderSettings openai = new ProviderSettings();
        private ProviderSettings anthropic = new ProviderSettings();
    }

    @Getter
    @Setter
    public static class ProviderSettings {
        private String apiKey;
        private String baseUrl;
        private String model;
        private int maxTokens = 2048;
        private double temperature = 0.3;
    }

    @Getter
    @Setter
    public static class Audit {
        private ProviderType provider = ProviderType.ANTHROPIC;
        private boolean strictMode = false;
        private long timeoutMs = 30000;
    }

    @Getter
    @Setter
    public static class Compliance {
        private String rulesPath = "classpath:rules/sec-finra-rules.json";
    }
}
