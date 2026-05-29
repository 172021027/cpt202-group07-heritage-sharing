package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.controller.UserController;
import com.example.heritage_sharing_api.dto.RejectedSubmissionEditResponse;
import com.example.heritage_sharing_api.dto.SubmitResourceRequest;
import com.example.heritage_sharing_api.dto.UserProfileResponse;
import com.example.heritage_sharing_api.dto.UserResponse;
import com.example.heritage_sharing_api.dto.UserSubmissionResponse;
import com.example.heritage_sharing_api.dto.UserStatsResponse;
import com.example.heritage_sharing_api.entity.ContributorRequestStatus;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.service.ResourceService;
import com.example.heritage_sharing_api.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

class UserControllerTest {

    @Test
    void userResponseFromNormalProfileMapsAllFields() {
        User input = user(21L, "ProfileUser", "profile@test.com", "Female",
                "bio", "/uploads/avatars/p.png", UserRole.USER, ContributorRequestStatus.PENDING);

        UserResponse actualResult = UserResponse.from(input);

        Assertions.assertEquals(21L, actualResult.getId());
        Assertions.assertEquals("ProfileUser", actualResult.getUsername());
        Assertions.assertEquals("profile@test.com", actualResult.getEmail());
        Assertions.assertEquals("Female", actualResult.getGender());
        Assertions.assertEquals("bio", actualResult.getPersonalDescription());
        Assertions.assertEquals("/uploads/avatars/p.png", actualResult.getProfilePictureUrl());
        Assertions.assertEquals("user", actualResult.getRole());
        Assertions.assertEquals("pending", actualResult.getRoleRequestStatus());
    }

    @Test
    void userResponseFromEmptyInputReturnsNull() {
        User input = null;
        UserResponse expectedResult = null;

        UserResponse actualResult = UserResponse.from(input);

        Assertions.assertNull(actualResult);
        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void profileResponseWithBoundaryStatsKeepsZeroValues() {
        UserStatsResponse inputStats = new UserStatsResponse(0L, 0L, 0L);
        UserProfileResponse expectedResult = new UserProfileResponse(0L, "", null, "", null, inputStats);

        UserProfileResponse actualResult = new UserProfileResponse(0L, "", null, "", null, inputStats);

        Assertions.assertEquals(expectedResult.getUserId(), actualResult.getUserId());
        Assertions.assertEquals("", actualResult.getUsername());
        Assertions.assertNull(actualResult.getGender());
        Assertions.assertEquals(0L, actualResult.getResourceStats().getPending());
        Assertions.assertEquals(0L, actualResult.getResourceStats().getApproved());
        Assertions.assertEquals(0L, actualResult.getResourceStats().getRejected());
    }

    @Test
    void getCurrentUserWithNormalInputReturnsProfileBody() {
        User input = user(22L, "Current", "current@test.com", "Male",
                "current bio", "/avatar.png", UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService service = new FakeUserService();
        service.currentUser = input;
        UserController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.getCurrentUser();
        UserResponse actualBody = (UserResponse) actualResult.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertNotNull(actualBody);
        Assertions.assertEquals("Current", actualBody.getUsername());
        Assertions.assertEquals("contributor", actualBody.getRole());
        Assertions.assertEquals("approved", actualBody.getRoleRequestStatus());
    }

    @Test
    void getCurrentUserWithInvalidStateReturnsUnauthorized() {
        FakeUserService service = new FakeUserService();
        service.currentUser = null;
        UserController controller = controllerWith(service);
        String expectedResult = "User not authenticated";

        ResponseEntity<?> actualResult = controller.getCurrentUser();

        Assertions.assertEquals(HttpStatusCode.valueOf(401), actualResult.getStatusCode());
        Assertions.assertEquals(expectedResult, actualResult.getBody());
    }

    @Test
    void updateCurrentUserWithNormalInputReturnsSuccess() {
        User inputUser = user(23L, "Old", "old@test.com", "Male",
                "old bio", null, UserRole.USER, ContributorRequestStatus.NONE);
        UserController.UpdateUserRequest inputRequest = updateRequest("New", "Female", "new bio");
        User expectedUpdatedUser = user(23L, "New", "old@test.com", "Female",
                "new bio", null, UserRole.USER, ContributorRequestStatus.NONE);
        FakeUserService service = new FakeUserService();
        service.currentUser = inputUser;
        service.updatedUser = expectedUpdatedUser;
        UserController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.updateCurrentUser(inputRequest);

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertEquals("Profile updated successfully", actualResult.getBody());
        Assertions.assertEquals(23L, service.lastUpdateUserId);
        Assertions.assertEquals("New", service.lastUsername);
        Assertions.assertEquals("Female", service.lastGender);
        Assertions.assertEquals("new bio", service.lastPersonalDescription);
    }

    @Test
    void updateCurrentUserWithEmptyInputStillDelegatesProfileUpdate() {
        User inputUser = user(24L, "Empty", "empty@test.com", null,
                null, null, UserRole.USER, ContributorRequestStatus.NONE);
        UserController.UpdateUserRequest inputRequest = updateRequest(null, null, null);
        FakeUserService service = new FakeUserService();
        service.currentUser = inputUser;
        service.updatedUser = inputUser;
        UserController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.updateCurrentUser(inputRequest);

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertEquals("Profile updated successfully", actualResult.getBody());
        Assertions.assertNull(service.lastUsername);
        Assertions.assertNull(service.lastGender);
        Assertions.assertNull(service.lastPersonalDescription);
    }

    @Test
    void updateCurrentUserWithAbnormalServiceResultReturnsServerError() {
        User inputUser = user(25L, "Broken", "broken@test.com", null,
                null, null, UserRole.USER, ContributorRequestStatus.NONE);
        UserController.UpdateUserRequest inputRequest = updateRequest("Broken", "Unknown", "bio");
        FakeUserService service = new FakeUserService();
        service.currentUser = inputUser;
        service.updatedUser = null;
        UserController controller = controllerWith(service);
        String expectedResult = "Failed to update profile";

        ResponseEntity<?> actualResult = controller.updateCurrentUser(inputRequest);

        Assertions.assertEquals(HttpStatusCode.valueOf(500), actualResult.getStatusCode());
        Assertions.assertEquals(expectedResult, actualResult.getBody());
    }

    @Test
    void updateCurrentUserWithoutAuthenticatedUserReturnsUnauthorized() {
        UserController.UpdateUserRequest inputRequest = updateRequest("Nobody", "Male", "bio");
        FakeUserService service = new FakeUserService();
        service.currentUser = null;
        UserController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.updateCurrentUser(inputRequest);

        Assertions.assertEquals(HttpStatusCode.valueOf(401), actualResult.getStatusCode());
        Assertions.assertEquals("User not authenticated", actualResult.getBody());
        Assertions.assertNull(service.lastUpdateUserId);
    }

    @Test
    void requestContributorRoleWithNormalInputReturnsPendingProfileState() {
        User inputUser = user(26L, "Applicant", "app@test.com", null,
                null, null, UserRole.USER, ContributorRequestStatus.NONE);
        User updatedUser = user(26L, "Applicant", "app@test.com", null,
                null, null, UserRole.USER, ContributorRequestStatus.PENDING);
        FakeUserService service = new FakeUserService();
        service.currentUser = inputUser;
        service.contributorRequestResult = updatedUser;
        UserController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.requestContributorRole();
        Map<String, Object> actualBody = body(actualResult);

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertEquals("user", actualBody.get("role"));
        Assertions.assertEquals("pending", actualBody.get("roleRequestStatus"));
        Assertions.assertEquals("Contributor request submitted.", actualBody.get("message"));
    }

    @Test
    void requestContributorRoleWithAlreadyPendingStateReturnsConflict() {
        User inputUser = user(27L, "Pending", "pending@test.com", null,
                null, null, UserRole.USER, ContributorRequestStatus.PENDING);
        FakeUserService service = new FakeUserService();
        service.currentUser = inputUser;
        service.contributorRequestError = new IllegalStateException("Your contributor request is already pending admin approval.");
        UserController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.requestContributorRole();

        Assertions.assertEquals(HttpStatusCode.valueOf(409), actualResult.getStatusCode());
        Assertions.assertEquals("Your contributor request is already pending admin approval.", actualResult.getBody());
    }

    @Test
    void requestContributorRoleWithInvalidRoleReturnsBadRequest() {
        User inputUser = user(28L, "Admin", "admin@test.com", null,
                null, null, UserRole.ADMIN, ContributorRequestStatus.NONE);
        FakeUserService service = new FakeUserService();
        service.currentUser = inputUser;
        service.contributorRequestError = new IllegalStateException("Only User accounts can request Contributor access");
        UserController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.requestContributorRole();

        Assertions.assertEquals(HttpStatusCode.valueOf(400), actualResult.getStatusCode());
        Assertions.assertEquals("Only User accounts can request Contributor access", actualResult.getBody());
    }

    @Test
    void getCurrentUserSubmissionsReturnsSuccessWhenAuthenticated() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        UserSubmissionResponse submission = new UserSubmissionResponse();
        submission.setResourceId(10L);
        submission.setTitle("Shadow Puppetry");
        submission.setStatus("pending_review");
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.submissions = List.of(submission);
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.getCurrentUserSubmissions();
        List<?> body = (List<?>) response.getBody();
        UserSubmissionResponse actual = (UserSubmissionResponse) body.get(0);

        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        Assertions.assertEquals(1, body.size());
        Assertions.assertEquals(10L, actual.getResourceId());
        Assertions.assertEquals("Shadow Puppetry", actual.getTitle());
        Assertions.assertEquals("pending_review", actual.getStatus());
    }

    @Test
    void getCurrentUserSubmissionsReturnsUnauthorizedWhenUnauthenticated() {
        FakeUserService userService = new FakeUserService();
        UserController controller = controllerWith(userService, new FakeResourceService());

        ResponseEntity<?> response = controller.getCurrentUserSubmissions();

        Assertions.assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
        Assertions.assertEquals("User not authenticated", response.getBody());
    }

    @Test
    void getCurrentUserSubmissionForEditReturnsRejectedSubmissionDetails() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        RejectedSubmissionEditResponse editResponse = new RejectedSubmissionEditResponse();
        editResponse.setResourceId(10L);
        editResponse.setTitle("Shadow Puppetry");
        editResponse.setStatus("rejected");
        editResponse.setFeedback("Please add source details.");
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.editResponse = editResponse;
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.getCurrentUserSubmissionForEdit(10L);
        RejectedSubmissionEditResponse actual = (RejectedSubmissionEditResponse) response.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        Assertions.assertEquals(10L, actual.getResourceId());
        Assertions.assertEquals("Shadow Puppetry", actual.getTitle());
        Assertions.assertEquals("rejected", actual.getStatus());
        Assertions.assertEquals("Please add source details.", actual.getFeedback());
    }

    @Test
    void getCurrentUserSubmissionForEditReturnsForbiddenWhenUserIsNotContributor() {
        User regularUser = user(2L, "RegularUser", "user@example.com", null,
                null, null, UserRole.USER, ContributorRequestStatus.NONE);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = regularUser;
        UserController controller = controllerWith(userService, new FakeResourceService());

        ResponseEntity<?> response = controller.getCurrentUserSubmissionForEdit(10L);

        Assertions.assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
        Assertions.assertEquals("Only Contributor users can view submissions", response.getBody());
    }

    @Test
    void getCurrentUserSubmissionForEditReturnsNotFoundWhenResourceDoesNotExist() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.editException = new NoSuchElementException("Resource not found.");
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.getCurrentUserSubmissionForEdit(999L);
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
        Assertions.assertEquals(false, body.get("success"));
        Assertions.assertEquals("Resource not found.", body.get("message"));
    }

    @Test
    void getCurrentUserSubmissionForEditReturnsConflictWhenResourceIsNotRejected() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.editException = new IllegalStateException("Only rejected resources can be revised.");
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.getCurrentUserSubmissionForEdit(10L);
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(409), response.getStatusCode());
        Assertions.assertEquals(false, body.get("success"));
        Assertions.assertEquals("Only rejected resources can be revised.", body.get("message"));
    }

    @Test
    void resubmitCurrentUserSubmissionReturnsSuccessWhenValid() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.savedResource = savedResource();
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.resubmitCurrentUserSubmission(
                10L,
                "Shadow Puppetry Revised",
                "Xi'an",
                3L,
                "Updated archive",
                List.of("performance", "intangible"),
                "I own the rights",
                null,
                null
        );
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        Assertions.assertEquals(true, body.get("success"));
        Assertions.assertEquals("Resource resubmitted for review.", body.get("message"));
        Assertions.assertEquals(10L, body.get("resourceId"));
        Assertions.assertEquals("pending_review", body.get("status"));
        Assertions.assertEquals(1L, resourceService.lastResubmitContributorId);
        Assertions.assertEquals(10L, resourceService.lastResubmitResourceId);
    }

    @Test
    void resubmitCurrentUserSubmissionReturnsUnauthorizedWhenUnauthenticated() {
        FakeUserService userService = new FakeUserService();
        UserController controller = controllerWith(userService, new FakeResourceService());

        ResponseEntity<?> response = controller.resubmitCurrentUserSubmission(
                10L,
                "Shadow Puppetry Revised",
                "Xi'an",
                3L,
                "Updated archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );

        Assertions.assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
        Assertions.assertEquals("User not authenticated", response.getBody());
    }

    @Test
    void resubmitCurrentUserSubmissionReturnsForbiddenWhenUserIsNotContributor() {
        User regularUser = user(2L, "RegularUser", "user@example.com", null,
                null, null, UserRole.USER, ContributorRequestStatus.NONE);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = regularUser;
        UserController controller = controllerWith(userService, new FakeResourceService());

        ResponseEntity<?> response = controller.resubmitCurrentUserSubmission(
                10L,
                "Shadow Puppetry Revised",
                "Xi'an",
                3L,
                "Updated archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );

        Assertions.assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
        Assertions.assertEquals("Only Contributor users can resubmit resources", response.getBody());
    }

    @Test
    void resubmitCurrentUserSubmissionReturnsNotFoundWhenResourceDoesNotExist() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.resubmitException = new NoSuchElementException("Resource not found.");
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.resubmitCurrentUserSubmission(
                999L,
                "Shadow Puppetry Revised",
                "Xi'an",
                3L,
                "Updated archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
        Assertions.assertEquals(false, body.get("success"));
        Assertions.assertEquals("Resource not found.", body.get("message"));
    }

    @Test
    void resubmitCurrentUserSubmissionReturnsConflictWhenResourceIsNotRejected() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.resubmitException = new IllegalStateException("Only rejected resources can be resubmitted.");
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.resubmitCurrentUserSubmission(
                10L,
                "Shadow Puppetry Revised",
                "Xi'an",
                3L,
                "Updated archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(409), response.getStatusCode());
        Assertions.assertEquals(false, body.get("success"));
        Assertions.assertEquals("Only rejected resources can be resubmitted.", body.get("message"));
    }

    @Test
    void resubmitCurrentUserSubmissionReturnsBadRequestWhenValidationFails() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.resubmitException = new IllegalArgumentException("Image is required.");
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.resubmitCurrentUserSubmission(
                10L,
                "Shadow Puppetry Revised",
                "Xi'an",
                3L,
                "Updated archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        Assertions.assertEquals(false, body.get("success"));
        Assertions.assertEquals("Image is required.", body.get("message"));
    }

    @Test
    void resubmitCurrentUserSubmissionReturnsBadRequestWhenTitleIsEmpty() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.resubmitException = new IllegalArgumentException("Title is required.");
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.resubmitCurrentUserSubmission(
                10L,
                "",
                "Xi'an",
                3L,
                "Updated archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        Assertions.assertEquals(false, body.get("success"));
        Assertions.assertEquals("Title is required.", body.get("message"));
    }

    @Test
    void resubmitCurrentUserSubmissionReturnsBadRequestWhenTagsAreEmpty() {
        User contributor = user(1L, "Contributor", "contributor@example.com", null,
                null, null, UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService userService = new FakeUserService();
        userService.currentUser = contributor;
        FakeResourceService resourceService = new FakeResourceService();
        resourceService.resubmitException = new IllegalArgumentException("At least one tag is required.");
        UserController controller = controllerWith(userService, resourceService);

        ResponseEntity<?> response = controller.resubmitCurrentUserSubmission(
                10L,
                "Shadow Puppetry Revised",
                "Xi'an",
                3L,
                "Updated archive",
                List.of(),
                "I own the rights",
                null,
                null
        );
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        Assertions.assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        Assertions.assertEquals(false, body.get("success"));
        Assertions.assertEquals("At least one tag is required.", body.get("message"));
    }

    private static UserController controllerWith(UserService userService) {
        UserController controller = new UserController();
        setField(controller, "userService", userService);
        return controller;
    }

    private static UserController controllerWith(UserService userService, ResourceService resourceService) {
        UserController controller = controllerWith(userService);
        setField(controller, "resourceService", resourceService);
        return controller;
    }

    private static UserController.UpdateUserRequest updateRequest(String username, String gender, String personalDescription) {
        UserController.UpdateUserRequest request = new UserController.UpdateUserRequest();
        request.setUsername(username);
        setField(request, "gender", gender);
        setField(request, "personalDescription", personalDescription);
        return request;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static User user(Long id, String username, String email, String gender,
                             String description, String avatar, UserRole role, ContributorRequestStatus status) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setGender(gender);
        user.setPersonalDescription(description);
        user.setProfilePicturePath(avatar);
        user.setRole(role);
        user.setRoleRequestStatus(status);
        return user;
    }

    private static Resource savedResource() {
        Resource resource = new Resource();
        resource.setResourceId(10L);
        resource.setContributorId(1L);
        resource.setTitle("Shadow Puppetry");
        resource.setLocation("Xi'an");
        resource.setCategoryId(3L);
        resource.setDescription("Traditional performance archive");
        resource.setCopyrightDeclaration("I own the rights");
        resource.setStatus(ResourceStatus.PENDING_REVIEW);
        return resource;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<?> response) {
        return (Map<String, Object>) response.getBody();
    }

    private static class FakeUserService extends UserService {
        private User currentUser;
        private User updatedUser;
        private User contributorRequestResult;
        private IllegalStateException contributorRequestError;
        private Long lastUpdateUserId;
        private String lastUsername;
        private String lastGender;
        private String lastPersonalDescription;

        @Override
        public User getCurrentUser() {
            return currentUser;
        }

        @Override
        public boolean isContributor(User user) {
            return user != null && user.getRole() == UserRole.CONTRIBUTOR;
        }

        @Override
        public User updateProfile(Long userId, String username, String gender, String personalDescription) {
            lastUpdateUserId = userId;
            lastUsername = username;
            lastGender = gender;
            lastPersonalDescription = personalDescription;
            return updatedUser;
        }

        @Override
        public User requestContributorRole(Long userId) {
            if (contributorRequestError != null) {
                throw contributorRequestError;
            }
            return contributorRequestResult;
        }
    }

    private static class FakeResourceService extends ResourceService {
        private Resource savedResource;
        private List<UserSubmissionResponse> submissions = List.of();
        private RejectedSubmissionEditResponse editResponse;
        private RuntimeException editException;
        private Exception resubmitException;
        private Long lastResubmitContributorId;
        private Long lastResubmitResourceId;

        @Override
        public List<UserSubmissionResponse> getContributorSubmissions(Long contributorId) {
            return submissions;
        }

        @Override
        public RejectedSubmissionEditResponse getRejectedSubmissionForEdit(Long contributorId, Long resourceId) {
            if (editException != null) {
                throw editException;
            }
            return editResponse;
        }

        @Override
        public Resource resubmitRejectedSubmission(Long contributorId,
                                                   Long resourceId,
                                                   SubmitResourceRequest request,
                                                   MultipartFile image,
                                                   MultipartFile video) throws IOException {
            lastResubmitContributorId = contributorId;
            lastResubmitResourceId = resourceId;
            if (resubmitException instanceof IOException ioException) {
                throw ioException;
            }
            if (resubmitException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            return savedResource;
        }
    }
}
