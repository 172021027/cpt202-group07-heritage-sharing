package com.example.heritage_sharing_api.entity;

import java.util.Locale;

// Canonical status values for resources.
// Each enum constant keeps both database and frontend values.
public enum ResourceStatus {
    DRAFT("Draft", "draft"),
    PENDING_REVIEW("Pending Review", "pending_review"),
    REJECTED("Rejected", "rejected"),
    APPROVED("Approved", "approved"),
    ARCHIVED("Archived", "archived"),
    UNPUBLISHED("Unpublished", "unpublished"),
    DELETED("Deleted", "deleted");

    private final String dbValue;
    private final String frontendValue;

    ResourceStatus(String dbValue, String frontendValue) {
        this.dbValue = dbValue;
        this.frontendValue = frontendValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getFrontendValue() {
        return frontendValue;
    }

    // Convert a raw database value or legacy status string to a canonical enum.
    public static ResourceStatus fromDbValue(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        String trimmed = rawValue.trim();
        for (ResourceStatus status : values()) {
            if (status.dbValue.equalsIgnoreCase(trimmed) || status.name().equalsIgnoreCase(trimmed)) {
                return status;
            }
        }

        // Backward compatibility for legacy values already stored in DB.
        String normalized = trimmed.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PENDING", "PENDING_REVIEW" -> PENDING_REVIEW;
            case "APPROVED" -> APPROVED;
            case "REJECTED" -> REJECTED;
            case "DRAFT" -> DRAFT;
            case "ARCHIVED" -> ARCHIVED;
            case "UNPUBLISHED", "OFFLINE" -> UNPUBLISHED;
            case "DELETED" -> DELETED;
            default -> throw new IllegalArgumentException("Unknown resource status: " + rawValue);
        };
    }
}
