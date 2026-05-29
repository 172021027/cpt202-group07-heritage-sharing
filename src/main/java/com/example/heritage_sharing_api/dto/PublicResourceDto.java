package com.example.heritage_sharing_api.dto;

import java.util.List;

public record PublicResourceDto(
        Long resourceId,
        Long contributorId,
        Long categoryId,
        String title,
        String description,
        String category,
        List<String> tags,
        String location,
        String picturePath,
        String videoPath,
        String background,
        String contributors,
        String date,
        String status
) {
}
