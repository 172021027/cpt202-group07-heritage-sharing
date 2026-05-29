package com.example.heritage_sharing_api.dto;

import com.example.heritage_sharing_api.entity.ResourceActionType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ResourceActionResponse {
    private Long actionId;
    private Long resourceId;
    private String resourceTitle;
    private String actionType;
    private Long actionByUserId;
    private String actionByUserName;
    private String actionAt;
    private String feedbackText;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ResourceActionResponse() {}

    public ResourceActionResponse(Long actionId, Long resourceId, String resourceTitle, String actionType,
                                 Long actionByUserId, String actionByUserName, String actionAt, String feedbackText) {
        this.actionId = actionId;
        this.resourceId = resourceId;
        this.resourceTitle = resourceTitle;
        this.actionType = actionType;
        this.actionByUserId = actionByUserId;
        this.actionByUserName = actionByUserName;
        this.actionAt = actionAt;
        this.feedbackText = feedbackText;
    }

    public ResourceActionResponse(Long actionId, Long resourceId, String resourceTitle, String actionType,
                                 Long actionByUserId, String actionByUserName, LocalDateTime actionAt, String feedbackText) {
        this.actionId = actionId;
        this.resourceId = resourceId;
        this.resourceTitle = resourceTitle;
        this.actionType = actionType;
        this.actionByUserId = actionByUserId;
        this.actionByUserName = actionByUserName;
        this.actionAt = actionAt != null ? actionAt.format(FORMATTER) : "";
        this.feedbackText = feedbackText;
    }

    public ResourceActionResponse(Long actionId, Long resourceId, String resourceTitle, ResourceActionType actionType,
                                 Long actionByUserId, String actionByUserName, LocalDateTime actionAt, String feedbackText) {
        this.actionId = actionId;
        this.resourceId = resourceId;
        this.resourceTitle = resourceTitle;
        this.actionType = actionType == null ? "" : actionType.name();
        this.actionByUserId = actionByUserId;
        this.actionByUserName = actionByUserName;
        this.actionAt = actionAt != null ? actionAt.format(FORMATTER) : "";
        this.feedbackText = feedbackText;
    }

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getResourceTitle() { return resourceTitle; }
    public void setResourceTitle(String resourceTitle) { this.resourceTitle = resourceTitle; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Long getActionByUserId() { return actionByUserId; }
    public void setActionByUserId(Long actionByUserId) { this.actionByUserId = actionByUserId; }

    public String getActionByUserName() { return actionByUserName; }
    public void setActionByUserName(String actionByUserName) { this.actionByUserName = actionByUserName; }

    public String getActionAt() { return actionAt; }
    public void setActionAt(String actionAt) { this.actionAt = actionAt; }

    public String getFeedbackText() { return feedbackText; }
    public void setFeedbackText(String feedbackText) { this.feedbackText = feedbackText; }
}
