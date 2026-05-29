package com.example.heritage_sharing_api.dto;

public class UpdateUserProfileRequest {
    private String username;
    private String gender;
    private String personalDescription;

    public UpdateUserProfileRequest() {
    }

    public UpdateUserProfileRequest(String username, String gender, String personalDescription) {
        this.username = username;
        this.gender = gender;
        this.personalDescription = personalDescription;
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
}
