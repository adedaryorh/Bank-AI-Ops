package com.bankops.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bankops")
public record BankOpsProperties(Security security, Cors cors, String notice) {

    public record Security(String audience, String issuerUri, String jwkSetUri) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
