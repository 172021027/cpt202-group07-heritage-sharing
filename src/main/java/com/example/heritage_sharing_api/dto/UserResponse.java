package com.example.heritage_sharing_api.dto;

import com.example.heritage_sharing_api.entity.User;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String gender;
    private String role;
    private String roleRequestStatus;
    private String personalDescription;
    private String profilePictureUrl;

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setGender(user.getGender());
        response.setRole(user.getRole().getApiValue());
        response.setRoleRequestStatus(user.getRoleRequestStatus().getApiValue());
        response.setPersonalDescription(user.getPersonalDescription());
        response.setProfilePictureUrl(user.getProfilePicturePath());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getRoleRequestStatus() { return roleRequestStatus; }
    public void setRoleRequestStatus(String roleRequestStatus) { this.roleRequestStatus = roleRequestStatus; }

    public String getPersonalDescription() { return personalDescription; }
    public void setPersonalDescription(String personalDescription) { this.personalDescription = personalDescription; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
}
