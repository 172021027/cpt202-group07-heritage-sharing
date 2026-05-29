package com.example.heritage_sharing_api.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Convert between ResourceStatus and the database ENUM text value.
@Converter(autoApply = false)
public class ResourceStatusConverter implements AttributeConverter<ResourceStatus, String> {
    // Write enum values as the configured database string.
    @Override
    public String convertToDatabaseColumn(ResourceStatus attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    // Read database text and resolve it to a canonical enum value.
    @Override
    public ResourceStatus convertToEntityAttribute(String dbData) {
        return ResourceStatus.fromDbValue(dbData);
    }
}
