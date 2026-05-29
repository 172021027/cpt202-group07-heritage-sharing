package com.example.heritage_sharing_api.dto;

public class UserProfileResponse {
    private Long userId;
    private String username;
    private String gender;
    private String personalDescription;
    private String profilePictureUrl;
    private UserStatsResponse resourceStats;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long userId, String username, String gender, String personalDescription, String profilePictureUrl, UserStatsResponse resourceStats) {
        this.userId = userId;
        this.username = username;
        this.gender = gender;
        this.personalDescription = personalDescription;
        this.profilePictureUrl = profilePictureUrl;
        this.resourceStats = resourceStats;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPersonalDescription() {
        return personalDescription;
    }

    public void setPersonalDescription(String personalDescription) {
        this.personalDescription = personalDescription;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public UserStatsResponse getResourceStats() {
        return resourceStats;
    }

    public void setResourceStats(UserStatsResponse resourceStats) {
        this.resourceStats = resourceStats;
    }
}
