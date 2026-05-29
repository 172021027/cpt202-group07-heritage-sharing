package com.example.heritage_sharing_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "contributor_id", nullable = false)
    private Long contributorId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "picture_path", length = 500)
    private String picturePath;

    @Column(name = "video_path", length = 500)
    private String videoPath;

    @Column(name = "copyright_declaration", columnDefinition = "TEXT")
    private String copyrightDeclaration;

    @Column(name = "status", nullable = false)
    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public Resource() {
    }

    public Resource(Long resourceId,
                    Long contributorId,
                    String title,
                    String description,
                    Long categoryId,
                    String location,
                    String picturePath,
                    String videoPath,
                    String copyrightDeclaration,
                    ResourceStatus status,
                    LocalDateTime submittedAt,
                    LocalDateTime approvedAt) {
        this.resourceId = resourceId;
        this.contributorId = contributorId;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.location = location;
        this.picturePath = picturePath;
        this.videoPath = videoPath;
        this.copyrightDeclaration = copyrightDeclaration;
        this.status = status;
        this.submittedAt = submittedAt;
        this.approvedAt = approvedAt;
    }

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

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
