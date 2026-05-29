package com.example.heritage_sharing_api.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ContributorRequestStatusConverter implements AttributeConverter<ContributorRequestStatus, String> {
    @Override
    public String convertToDatabaseColumn(ContributorRequestStatus attribute) {
        return (attribute == null ? ContributorRequestStatus.NONE : attribute).getApiValue();
    }

    @Override
    public ContributorRequestStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            throw new IllegalStateException("Database value for users.role_request_status must not be null or blank.");
        }
        return ContributorRequestStatus.fromValue(dbData);
    }
}
