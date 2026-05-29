package com.example.heritage_sharing_api.dto;

public class ResourceCommentResponse {
    private Long commentId;
    private Long resourceId;
    private Long userId;
    private String username;
    private String commentText;
    private String commentedAt;
    private boolean canDelete;

    // Constructor for @Query mapping - supports optimized database queries
    public ResourceCommentResponse(Long commentId, Long resourceId, Long userId, 
                                   String username, String commentText, String commentedAt, 
                                   boolean canDelete) {
        this.commentId = commentId;
        this.resourceId = resourceId;
        this.userId = userId;
        this.username = username;
        this.commentText = commentText;
        this.commentedAt = commentedAt;
        this.canDelete = canDelete;
    }

    // Default constructor for JPA and other frameworks
    public ResourceCommentResponse() {}

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getCommentedAt() {
        return commentedAt;
    }

    public void setCommentedAt(String commentedAt) {
        this.commentedAt = commentedAt;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }
}
