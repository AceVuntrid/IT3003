package com.university.assets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Storage storage,
        Security security,
        Seed seed,
        Reservation reservation
) {
    public record Jwt(String secret, long accessExpiryMinutes, long refreshExpiryDays) {}
    public record Cors(List<String> allowedOrigins) {}
    public record Storage(String type, String path) {}
    public record Security(int failedLoginLimit, int lockMinutes) {}
    public record Seed(String adminEmail, String adminPassword, boolean demoData) {}
    public record Reservation(BigDecimal overduePenaltyPerDay) {}
}
