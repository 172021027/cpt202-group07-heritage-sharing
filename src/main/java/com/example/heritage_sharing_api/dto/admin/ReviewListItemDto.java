package com.example.heritage_sharing_api.dto.admin;

import java.util.List;

public class ReviewListItemDto {
    private Long resourceId;
    private String title;
    private String category;
    private String status;
    private List<ReviewSubmissionDto> submissions;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ReviewSubmissionDto> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<ReviewSubmissionDto> submissions) {
        this.submissions = submissions;
    }
}
