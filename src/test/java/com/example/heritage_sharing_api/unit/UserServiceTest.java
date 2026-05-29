package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.dto.UserStatsResponse;
import com.example.heritage_sharing_api.entity.ContributorRequestStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.repository.ResourceRepository;
import com.example.heritage_sharing_api.repository.UserRepository;
import com.example.heritage_sharing_api.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserReturnsUserWhenAuthenticatedPrincipalIsLong() {
        User user = user(11L, "Current", UserRole.USER, ContributorRequestStatus.NONE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(11L, null, List.of())
        );
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));

        User actual = userService.getCurrentUser();

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(11L, actual.getUserId());
        Assertions.assertEquals("Current", actual.getUsername());
    }

    @Test
    void getCurrentUserReturnsNullWhenAuthenticationIsMissing() {
        User actual = userService.getCurrentUser();

        Assertions.assertNull(actual);
        verify(userRepository, never()).findById(1L);
    }

    @Test
    void getCurrentUserReturnsNullWhenPrincipalIsNotLong() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("11", null)
        );

        User actual = userService.getCurrentUser();

        Assertions.assertNull(actual);
    }

    @Test
    void isContributorReturnsTrueOnlyForContributorRole() {
        User contributor = user(1L, "Contributor", UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED);
        User regularUser = user(2L, "User", UserRole.USER, ContributorRequestStatus.NONE);

        Assertions.assertTrue(userService.isContributor(contributor));
        Assertions.assertFalse(userService.isContributor(regularUser));
        Assertions.assertFalse(userService.isContributor(null));
    }

    @Test
    void requestContributorRolePromotesEligibleUserToPending() {
        User applicant = user(21L, "Applicant", UserRole.USER, ContributorRequestStatus.NONE);
        when(userRepository.findById(21L)).thenReturn(Optional.of(applicant));
        when(userRepository.save(applicant)).thenReturn(applicant);

        User actual = userService.requestContributorRole(21L);

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(ContributorRequestStatus.PENDING, actual.getRoleRequestStatus());
        verify(userRepository).save(applicant);
    }

    @Test
    void requestContributorRoleAllowsRejectedUserToRetry() {
        User applicant = user(22L, "Applicant", UserRole.USER, ContributorRequestStatus.REJECTED);
        when(userRepository.findById(22L)).thenReturn(Optional.of(applicant));
        when(userRepository.save(applicant)).thenReturn(applicant);

        User actual = userService.requestContributorRole(22L);

        Assertions.assertEquals(ContributorRequestStatus.PENDING, actual.getRoleRequestStatus());
    }

    @Test
    void requestContributorRoleThrowsWhenUserIsMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> userService.requestContributorRole(99L)
        );

        Assertions.assertEquals("User not found", exception.getMessage());
    }

    @Test
    void requestContributorRoleThrowsWhenRoleIsNotRegularUser() {
        User admin = user(23L, "Admin", UserRole.ADMIN, ContributorRequestStatus.NONE);
        when(userRepository.findById(23L)).thenReturn(Optional.of(admin));

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> userService.requestContributorRole(23L)
        );

        Assertions.assertEquals("Only User accounts can request Contributor access", exception.getMessage());
    }

    @Test
    void requestContributorRoleThrowsWhenAlreadyPending() {
        User applicant = user(24L, "Pending", UserRole.USER, ContributorRequestStatus.PENDING);
        when(userRepository.findById(24L)).thenReturn(Optional.of(applicant));

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> userService.requestContributorRole(24L)
        );

        Assertions.assertEquals("Your contributor request is already pending admin approval.", exception.getMessage());
    }

    @Test
    void updateProfileUpdatesOnlyProvidedFieldsAndNormalizesGender() {
        User user = user(31L, "Old", UserRole.USER, ContributorRequestStatus.NONE);
        user.setGender("Male");
        user.setPersonalDescription("old bio");
        when(userRepository.findById(31L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User actual = userService.updateProfile(31L, "New", "", "new bio");

        Assertions.assertNotNull(actual);
        Assertions.assertEquals("New", actual.getUsername());
        Assertions.assertNull(actual.getGender());
        Assertions.assertEquals("new bio", actual.getPersonalDescription());
    }

    @Test
    void updateProfileLeavesExistingFieldsWhenInputsAreNull() {
        User user = user(32L, "Stable", UserRole.USER, ContributorRequestStatus.NONE);
        user.setGender("Female");
        user.setPersonalDescription("kept");
        when(userRepository.findById(32L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User actual = userService.updateProfile(32L, null, null, null);

        Assertions.assertEquals("Stable", actual.getUsername());
        Assertions.assertEquals("Female", actual.getGender());
        Assertions.assertEquals("kept", actual.getPersonalDescription());
    }

    @Test
    void updateProfileReturnsNullWhenUserDoesNotExist() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        User actual = userService.updateProfile(404L, "Nobody", "Male", "bio");

        Assertions.assertNull(actual);
    }

    @Test
    void registerCreatesRegularUserWithEncodedPassword() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User actual = userService.register("new@example.com", "password123", "NewUser");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        Assertions.assertNotNull(actual);
        Assertions.assertEquals("new@example.com", captor.getValue().getEmail());
        Assertions.assertEquals("encoded-password", captor.getValue().getPasswordHash());
        Assertions.assertEquals(UserRole.USER, captor.getValue().getRole());
        Assertions.assertEquals(ContributorRequestStatus.NONE, captor.getValue().getRoleRequestStatus());
    }

    @Test
    void registerReturnsNullWhenEmailAlreadyExists() {
        User existingUser = user(41L, "Existing", UserRole.USER, ContributorRequestStatus.NONE);
        existingUser.setEmail("existing@example.com");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        User actual = userService.register("existing@example.com", "password123", "Existing");

        Assertions.assertNull(actual);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void registerThrowsWhenPasswordIsTooShort() {
        when(userRepository.findByEmail("short@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("short@example.com", "short", "Shorty")
        );

        Assertions.assertEquals("Password must be at least 8 characters long", exception.getMessage());
    }

    @Test
    void loginReturnsUserWhenPasswordMatches() {
        User user = user(51L, "LoginUser", UserRole.USER, ContributorRequestStatus.NONE);
        user.setEmail("login@example.com");
        user.setPasswordHash("encoded");
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);

        User actual = userService.login("login@example.com", "password123");

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(51L, actual.getUserId());
    }

    @Test
    void loginReturnsNullWhenPasswordDoesNotMatch() {
        User user = user(52L, "LoginUser", UserRole.USER, ContributorRequestStatus.NONE);
        user.setEmail("login@example.com");
        user.setPasswordHash("encoded");
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded")).thenReturn(false);

        User actual = userService.login("login@example.com", "wrong-password");

        Assertions.assertNull(actual);
    }

    @Test
    void getUserStatsMapsRepositoryCountsAndDefaultsNullToZero() {
        when(resourceRepository.countByContributorIdAndMultipleStatuses(61L))
                .thenReturn(new Object[]{2L, null, 5L});

        UserStatsResponse actual = userService.getUserStats(61L);

        Assertions.assertEquals(2L, actual.getPending());
        Assertions.assertEquals(0L, actual.getApproved());
        Assertions.assertEquals(5L, actual.getRejected());
    }

    @Test
    void resetPasswordEncodesAndSavesWhenUserExists() {
        User user = user(71L, "ResetUser", UserRole.USER, ContributorRequestStatus.NONE);
        user.setEmail("reset@example.com");
        user.setPasswordHash("old-hash");
        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword123")).thenReturn("new-encoded");
        when(userRepository.save(user)).thenReturn(user);

        User actual = userService.resetPassword("reset@example.com", "newpassword123");

        Assertions.assertNotNull(actual);
        Assertions.assertEquals("new-encoded", actual.getPasswordHash());
    }

    @Test
    void resetPasswordThrowsWhenPasswordIsTooShort() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> userService.resetPassword("reset@example.com", "short")
        );

        Assertions.assertEquals("Password must be at least 8 characters long", exception.getMessage());
    }

    private static User user(Long id, String username, UserRole role, ContributorRequestStatus status) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setEmail(username.toLowerCase() + "@example.com");
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setRoleRequestStatus(status);
        return user;
    }
}
