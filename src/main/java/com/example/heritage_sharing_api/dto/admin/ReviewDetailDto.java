package com.example.heritage_sharing_api.dto.admin;

import java.util.List;

public class ReviewDetailDto {
    private Long resourceId;
    private Long contributorId;
    private String contributorUsername;
    private String title;
    private String description;
    private Long categoryId;
    private String category;
    private String location;
    private String picturePath;
    private String videoPath;
    private String copyrightDeclaration;
    private String status;
    private String submittedAt;
    private String approvedAt;
    private List<ReviewTagDto> tags;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getContributorId() {
        return contributorId;
    }

    public void setContributorId(Long contributorId) {
        this.contributorId = contributorId;
    }

    public String getContributorUsername() {
        return contributorUsername;
    }

    public void setContributorUsername(String contributorUsername) {
        this.contributorUsername = contributorUsername;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getCopyrightDeclaration() {
        return copyrightDeclaration;
    }

    public void setCopyrightDeclaration(String copyrightDeclaration) {
        this.copyrightDeclaration = copyrightDeclaration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(String approvedAt) {
        this.approvedAt = approvedAt;
    }

    public List<ReviewTagDto> getTags() {
        return tags;
    }

    public void setTags(List<ReviewTagDto> tags) {
        this.tags = tags;
    }

    public static class ReviewTagDto {
        private Long tagId;
        private String tagName;

        public Long getTagId() {
            return tagId;
        }

        public void setTagId(Long tagId) {
            this.tagId = tagId;
        }

        public String getTagName() {
            return tagName;
        }

        public void setTagName(String tagName) {
            this.tagName = tagName;
        }
    }
}
