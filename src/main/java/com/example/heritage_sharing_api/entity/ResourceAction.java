package com.example.heritage_sharing_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_actions")
public class ResourceAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceActionType actionType;

    @Column(name = "action_by_user_id", nullable = false)
    private Long actionByUserId;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    public ResourceAction() {}

    public ResourceAction(Long actionId, Long resourceId, ResourceActionType actionType, Long actionByUserId, LocalDateTime actionAt, String feedbackText) {
        this.actionId = actionId;
        this.resourceId = resourceId;
        this.actionType = actionType;
        this.actionByUserId = actionByUserId;
        this.actionAt = actionAt;
        this.feedbackText = feedbackText;
    }

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public ResourceActionType getActionType() { return actionType; }
    public void setActionType(ResourceActionType actionType) { this.actionType = actionType; }

    public Long getActionByUserId() { return actionByUserId; }
    public void setActionByUserId(Long actionByUserId) { this.actionByUserId = actionByUserId; }

    public LocalDateTime getActionAt() { return actionAt; }
    public void setActionAt(LocalDateTime actionAt) { this.actionAt = actionAt; }

    public String getFeedbackText() { return feedbackText; }
    public void setFeedbackText(String feedbackText) { this.feedbackText = feedbackText; }
}
