package com.example.heritage_sharing_api.dto;

import java.util.List;

public class SubmitResourceRequest {
    private String title;
    private String location;
    private Long categoryId;
    private String description;
    private List<String> tags;
    private String picturePath;
    private String videoPath;
    private String copyrightDeclaration;
    private Long contributorId;

    public SubmitResourceRequest() {}

    public SubmitResourceRequest(String title, String location, Long categoryId, String description, List<String> tags, String picturePath, String videoPath, String copyrightDeclaration, Long contributorId) {
        this.title = title;
        this.location = location;
        this.categoryId = categoryId;
        this.description = description;
        this.tags = tags;
        this.picturePath = picturePath;
        this.videoPath = videoPath;
        this.copyrightDeclaration = copyrightDeclaration;
        this.contributorId = contributorId;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getPicturePath() { return picturePath; }
    public void setPicturePath(String picturePath) { this.picturePath = picturePath; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public String getCopyrightDeclaration() { return copyrightDeclaration; }
    public void setCopyrightDeclaration(String copyrightDeclaration) { this.copyrightDeclaration = copyrightDeclaration; }

    public Long getContributorId() { return contributorId; }
    public void setContributorId(Long contributorId) { this.contributorId = contributorId; }
}
