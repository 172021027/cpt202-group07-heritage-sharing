package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.controller.AdminContributorRequestController;
import com.example.heritage_sharing_api.entity.ContributorRequestStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

class AdminContributorRequestControllerTest {

    @Test
    void getPendingRequestsWithNormalInputReturnsMappedUsers() {
        List<User> input = List.of(user(7L, "Alice", "alice@test.com", UserRole.USER, ContributorRequestStatus.PENDING));
        List<Map<String, Object>> expectedResult = List.of(Map.of(
                "id", 7L,
                "username", "Alice",
                "email", "alice@test.com",
                "role", "user",
                "roleRequestStatus", "pending"));
        AdminContributorRequestController controller = controllerWith(new FakeUserService(input, List.of()));

        ResponseEntity<List<Map<String, Object>>> actualResult = controller.getPendingRequests();

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertEquals(expectedResult, actualResult.getBody());
    }

    @Test
    void getPendingRequestsWithEmptyInputReturnsEmptyList() {
        List<User> input = List.of();
        List<Map<String, Object>> expectedResult = List.of();
        AdminContributorRequestController controller = controllerWith(new FakeUserService(input, List.of()));

        ResponseEntity<List<Map<String, Object>>> actualResult = controller.getPendingRequests();

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertEquals(expectedResult, actualResult.getBody());
    }

    @Test
    void getContributorsWithBoundaryIdReturnsMappedContributor() {
        List<User> input = List.of(user(0L, "Zero", "zero@test.com", UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED));
        AdminContributorRequestController controller = controllerWith(new FakeUserService(List.of(), input));

        ResponseEntity<List<Map<String, Object>>> actualResult = controller.getContributors();

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertEquals(1, actualResult.getBody().size());
        Assertions.assertEquals(0L, actualResult.getBody().get(0).get("id"));
        Assertions.assertEquals("contributor", actualResult.getBody().get(0).get("role"));
        Assertions.assertEquals("approved", actualResult.getBody().get(0).get("roleRequestStatus"));
    }

    @Test
    void approveRequestWithNormalInputReturnsApprovedUser() {
        Long input = 8L;
        User expectedUser = user(input, "Bob", "bob@test.com", UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        FakeUserService service = new FakeUserService(List.of(), List.of());
        service.approveResult = expectedUser;
        AdminContributorRequestController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.approveRequest(input);

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertEquals(input, body(actualResult).get("id"));
        Assertions.assertEquals("contributor", body(actualResult).get("role"));
        Assertions.assertEquals(input, service.lastApprovedId);
    }

    @Test
    void approveRequestWithAbnormalInputReturnsBadRequest() {
        Long input = 999L;
        String expectedResult = "Contributor request not found";
        FakeUserService service = new FakeUserService(List.of(), List.of());
        service.approveResult = null;
        AdminContributorRequestController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.approveRequest(input);

        Assertions.assertEquals(HttpStatusCode.valueOf(400), actualResult.getStatusCode());
        Assertions.assertEquals(expectedResult, actualResult.getBody());
        Assertions.assertEquals(input, service.lastApprovedId);
    }

    @Test
    void rejectRequestWithNormalInputReturnsRejectedUser() {
        Long input = 9L;
        User expectedUser = user(input, "Cara", "cara@test.com", UserRole.USER, ContributorRequestStatus.REJECTED);
        FakeUserService service = new FakeUserService(List.of(), List.of());
        service.rejectResult = expectedUser;
        AdminContributorRequestController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.rejectRequest(input);

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertEquals("rejected", body(actualResult).get("roleRequestStatus"));
        Assertions.assertEquals(input, service.lastRejectedId);
    }

    @Test
    void rejectRequestWithInvalidStateReturnsBadRequest() {
        Long input = 10L;
        String expectedResult = "Contributor request not found";
        FakeUserService service = new FakeUserService(List.of(), List.of());
        service.rejectResult = null;
        AdminContributorRequestController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.rejectRequest(input);

        Assertions.assertEquals(HttpStatusCode.valueOf(400), actualResult.getStatusCode());
        Assertions.assertEquals(expectedResult, actualResult.getBody());
    }

    @Test
    void revokeContributorWithNormalInputReturnsRevokedUser() {
        Long input = 11L;
        User expectedUser = user(input, "Dan", "dan@test.com", UserRole.USER, ContributorRequestStatus.REVOKED);
        FakeUserService service = new FakeUserService(List.of(), List.of());
        service.revokeResult = expectedUser;
        AdminContributorRequestController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.revokeContributor(input);

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actualResult.getStatusCode());
        Assertions.assertEquals("revoked", body(actualResult).get("roleRequestStatus"));
        Assertions.assertEquals(input, service.lastRevokedId);
    }

    @Test
    void revokeContributorWithInvalidStateReturnsBadRequest() {
        Long input = 12L;
        String expectedResult = "Contributor not found";
        FakeUserService service = new FakeUserService(List.of(), List.of());
        service.revokeResult = null;
        AdminContributorRequestController controller = controllerWith(service);

        ResponseEntity<?> actualResult = controller.revokeContributor(input);

        Assertions.assertEquals(HttpStatusCode.valueOf(400), actualResult.getStatusCode());
        Assertions.assertEquals(expectedResult, actualResult.getBody());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<?> response) {
        return (Map<String, Object>) response.getBody();
    }

    private static AdminContributorRequestController controllerWith(UserService userService) {
        AdminContributorRequestController controller = new AdminContributorRequestController();
        setField(controller, "userService", userService);
        return controller;
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

    private static User user(Long id, String username, String email, UserRole role, ContributorRequestStatus status) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setRoleRequestStatus(status);
        return user;
    }

    private static class FakeUserService extends UserService {
        private final List<User> pendingRequests;
        private final List<User> contributors;
        private User approveResult;
        private User rejectResult;
        private User revokeResult;
        private Long lastApprovedId;
        private Long lastRejectedId;
        private Long lastRevokedId;

        private FakeUserService(List<User> pendingRequests, List<User> contributors) {
            this.pendingRequests = pendingRequests;
            this.contributors = contributors;
        }

        @Override
        public List<User> getPendingContributorRequests() {
            return pendingRequests;
        }

        @Override
        public List<User> getContributors() {
            return contributors;
        }

        @Override
        public User approveContributorRequest(Long userId) {
            lastApprovedId = userId;
            return approveResult;
        }

        @Override
        public User rejectContributorRequest(Long userId) {
            lastRejectedId = userId;
            return rejectResult;
        }

        @Override
        public User revokeContributorAccess(Long userId) {
            lastRevokedId = userId;
            return revokeResult;
        }
    }
}
