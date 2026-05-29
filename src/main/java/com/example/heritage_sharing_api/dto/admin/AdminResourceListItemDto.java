package com.example.heritage_sharing_api.dto.admin;

import java.time.LocalDateTime;

public record AdminResourceListItemDto(
        Long resourceId,
        String title,
        Long categoryId,
        String categoryName,
        String status,
        LocalDateTime submittedAt
) {
}
