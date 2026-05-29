package com.example.heritage_sharing_api.entity;

import java.util.Locale;

public enum ContributorRequestStatus {
    NONE("none"),
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    REVOKED("revoked");

    private final String apiValue;

    ContributorRequestStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    public static ContributorRequestStatus fromValue(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        String normalized = rawValue.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "NONE" -> NONE;
            case "PENDING" -> PENDING;
            case "APPROVED" -> APPROVED;
            case "REJECTED" -> REJECTED;
            case "REVOKED" -> REVOKED;
            default -> throw new IllegalArgumentException("Unknown contributor request status: " + rawValue);
        };
    }
}
