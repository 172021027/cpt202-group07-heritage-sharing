package com.example.heritage_sharing_api.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {
    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        return (attribute == null ? UserRole.USER : attribute).getApiValue();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            throw new IllegalStateException("Database value for users.role must not be null or blank.");
        }
        return UserRole.fromValue(dbData);
    }
}
