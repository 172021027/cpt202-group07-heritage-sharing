package com.example.heritage_sharing_api.entity;

import java.util.Locale;

public enum UserRole {
    USER("user"),
    CONTRIBUTOR("contributor"),
    ADMIN("admin");

    private final String apiValue;

    UserRole(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getAuthority() {
        return "ROLE_" + name();
    }

    public boolean canUsePublicLogin() {
        return this == USER || this == CONTRIBUTOR;
    }

    public boolean canUseAdminLogin() {
        return this == ADMIN;
    }

    public static UserRole fromValue(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        String normalized = rawValue.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }

        return switch (normalized) {
            case "USER" -> USER;
            case "CONTRIBUTOR" -> CONTRIBUTOR;
            case "ADMIN" -> ADMIN;
            default -> throw new IllegalArgumentException("Unknown user role: " + rawValue);
        };
    }
}
