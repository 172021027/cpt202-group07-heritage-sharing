package com.example.heritage_sharing_api.dto.admin;

public class ReviewDecisionRequestDto {
    private Long resourceId;
    private String submissionId;
    private String decision;
    private String note;
    private Long actionByUserId;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getActionByUserId() {
        return actionByUserId;
    }

    public void setActionByUserId(Long actionByUserId) {
        this.actionByUserId = actionByUserId;
    }
}
