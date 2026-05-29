package com.example.heritage_sharing_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_comments")
public class ResourceComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "commented_at", nullable = false)
    private LocalDateTime commentedAt;

    public ResourceComment() {}

    public ResourceComment(Long commentId, Long resourceId, Long userId, String commentText, LocalDateTime commentedAt) {
        this.commentId = commentId;
        this.resourceId = resourceId;
        this.userId = userId;
        this.commentText = commentText;
        this.commentedAt = commentedAt;
    }

    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public LocalDateTime getCommentedAt() { return commentedAt; }
    public void setCommentedAt(LocalDateTime commentedAt) { this.commentedAt = commentedAt; }
}
