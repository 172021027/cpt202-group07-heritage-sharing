package com.example.heritage_sharing_api.integration.auth;

import com.example.heritage_sharing_api.entity.ContributorRequestStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.integration.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@DisplayName("Integration tests for user accounts")
public class UserAccountIntegrationTests extends IntegrationTestSupport {

    @Test
    @DisplayName("IT-AUTH-01: password login returns JWT for public user and rejects admin public login")
    public void passwordLoginReturnsJwtForPublicUserAndRejectsAdminOnPublicLogin() throws Exception {
        var userLogin = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", regularUser.getEmail(), "password", TEST_PASSWORD)))).andReturn();
        var adminPublicLogin = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", adminUser.getEmail(), "password", TEST_PASSWORD)))).andReturn();

        JsonNode body = objectMapper.readTree(userLogin.getResponse().getContentAsString());

        Assertions.assertEquals(200, userLogin.getResponse().getStatus());
        Assertions.assertEquals(true, body.hasNonNull("token"));
        Assertions.assertEquals("regular.integration@example.com", body.get("user").get("email").asText());
        Assertions.assertEquals(401, adminPublicLogin.getResponse().getStatus());
    }

    @Test
    @DisplayName("IT-AUTH-02: admin login allows admin and rejects regular user")
    public void adminLoginAllowsAdminButRejectsRegularUser() throws Exception {
        var adminLogin = mvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", adminUser.getEmail(), "password", TEST_PASSWORD)))).andReturn();
        var userLogin = mvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", regularUser.getEmail(), "password", TEST_PASSWORD)))).andReturn();

        Assertions.assertEquals(200, adminLogin.getResponse().getStatus());
        Assertions.assertEquals(401, userLogin.getResponse().getStatus());
    }

    @Test
    @DisplayName("IT-AUTH-03: checkEmail returns different responses for user admin and missing email")
    public void checkEmailReturnsDifferentResponsesForExistingUserAdminAndMissingEmail() throws Exception {
        var userEmail = mvc.perform(post("/api/auth/check-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", regularUser.getEmail())))).andReturn();
        var adminEmail = mvc.perform(post("/api/auth/check-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", adminUser.getEmail())))).andReturn();
        var missingEmail = mvc.perform(post("/api/auth/check-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "missing@example.com")))).andReturn();

        Assertions.assertEquals(200, userEmail.getResponse().getStatus());
        Assertions.assertEquals(403, adminEmail.getResponse().getStatus());
        Assertions.assertEquals(404, missingEmail.getResponse().getStatus());
    }

    @Test
    @DisplayName("IT-AUTH-04: current user can read and update profile through repository")
    public void currentUserCanReadAndUpdateProfileThroughSecurityServiceAndRepository() throws Exception {
        var readResult = mvc.perform(get("/api/users/current")
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        var updateResult = mvc.perform(put("/api/users/current")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", "UpdatedUser",
                        "gender", "Female",
                        "personalDescription", "Interested in heritage resources"
                )))
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        User updatedUser = userRepository.findById(regularUser.getUserId()).orElseThrow();

        Assertions.assertEquals(200, readResult.getResponse().getStatus());
        Assertions.assertEquals(200, updateResult.getResponse().getStatus());
        Assertions.assertEquals("UpdatedUser", updatedUser.getUsername());
        Assertions.assertEquals("Female", updatedUser.getGender());
        Assertions.assertEquals("Interested in heritage resources", updatedUser.getPersonalDescription());
    }

    @Test
    @DisplayName("IT-AUTH-05: current user can upload avatar and persist profile path")
    public void currentUserCanUploadAvatarAndPathIsPersisted() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar",
                "avatar.png",
                "image/png",
                "fake-image".getBytes()
        );

        var result = mvc.perform(multipart("/api/users/current/avatar")
                .file(avatar)
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        User updatedUser = userRepository.findById(regularUser.getUserId()).orElseThrow();

        Assertions.assertEquals(200, result.getResponse().getStatus());
        Assertions.assertEquals(true, body.get("profilePictureUrl").asText().startsWith("/uploads/avatars/"));
        Assertions.assertEquals(body.get("profilePictureUrl").asText(), updatedUser.getProfilePicturePath());
    }

    @Test
    @DisplayName("IT-AUTH-06: regular user can request contributor role and duplicate conflicts")
    public void regularUserCanRequestContributorRoleAndDuplicateRequestConflicts() throws Exception {
        var firstResult = mvc.perform(post("/api/users/current/contributor-request")
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        var duplicateResult = mvc.perform(post("/api/users/current/contributor-request")
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        User updatedUser = userRepository.findById(regularUser.getUserId()).orElseThrow();

        Assertions.assertEquals(200, firstResult.getResponse().getStatus());
        Assertions.assertEquals(409, duplicateResult.getResponse().getStatus());
        Assertions.assertEquals(UserRole.USER, updatedUser.getRole());
        Assertions.assertEquals(ContributorRequestStatus.PENDING, updatedUser.getRoleRequestStatus());
    }
}
