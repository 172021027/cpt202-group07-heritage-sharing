package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.controller.AuthController;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.security.JwtUtil;
import com.example.heritage_sharing_api.service.EmailVerificationService;
import com.example.heritage_sharing_api.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for AuthController")
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("TestUser");
        testUser.setRole(UserRole.USER);
        testUser.setRoleRequestStatus(com.example.heritage_sharing_api.entity.ContributorRequestStatus.NONE);
    }

    @Test
    @DisplayName("UT-AC-01: register returns success when all validations pass")
    void registerReturnsSuccessWhenAllValidationsPass() throws Exception {
        String email = "newuser@example.com";
        String password = "password123";
        String username = "NewUser";
        String code = "1234";

        when(emailVerificationService.verifyCode(email, code)).thenReturn(true);
        when(userService.register(email, password, username)).thenReturn(testUser);
        when(jwtUtil.generateToken(1L, UserRole.USER)).thenReturn("test-token");
        doNothing().when(emailVerificationService).removeCode(email);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", encryptPassword(password));
        request.put("username", username);
        request.put("verificationCode", code);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"token\":\"test-token\",\"user\":{\"id\":1,\"email\":\"test@example.com\",\"username\":\"TestUser\",\"role\":\"user\",\"roleRequestStatus\":\"none\",\"profilePictureUrl\":null}}"));

        verify(emailVerificationService).verifyCode(email, code);
        verify(userService).register(email, password, username);
        verify(jwtUtil).generateToken(1L, UserRole.USER);
        verify(emailVerificationService).removeCode(email);
    }

    @Test
    @DisplayName("UT-AC-02: register returns 400 when required fields are missing")
    void registerReturnsBadRequestWhenFieldsMissing() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");
        // Missing password, username, verificationCode

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing required fields"));
    }

    @Test
    @DisplayName("UT-AC-03: register returns 400 when verification code is invalid")
    void registerReturnsBadRequestWhenVerificationCodeInvalid() throws Exception {
        String email = "test@example.com";

        when(emailVerificationService.verifyCode(email, "1234")).thenReturn(false);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", "password123");
        request.put("username", "TestUser");
        request.put("verificationCode", "1234");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid verification code"));

        verify(userService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("UT-AC-04: register returns 409 when email already exists")
    void registerReturnsConflictWhenEmailExists() throws Exception {
        String email = "existing@example.com";
        String password = "password123";

        when(emailVerificationService.verifyCode(email, "1234")).thenReturn(true);
        when(userService.register(email, password, "ExistingUser")).thenReturn(null);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", encryptPassword(password));
        request.put("username", "ExistingUser");
        request.put("verificationCode", "1234");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Email already exists"));
    }


    @Test
    @DisplayName("UT-AC-05: sendVerificationCode returns 400 when email is missing")
    void sendVerificationCodeReturnsBadRequestWhenEmailMissing() throws Exception {
        Map<String, String> request = new HashMap<>();
        // Missing email

        mockMvc.perform(post("/api/auth/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing email"));
    }

    @Test
    @DisplayName("UT-AC-06: login with password returns success when credentials are valid")
    void loginWithPasswordReturnsSuccessWhenValid() throws Exception {
        String email = "test@example.com";
        String password = encryptPassword("password123");

        when(userService.login(email, "password123")).thenReturn(testUser);
        when(jwtUtil.generateToken(1L, UserRole.USER)).thenReturn("test-token");

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"token\":\"test-token\",\"user\":{\"id\":1,\"email\":\"test@example.com\",\"username\":\"TestUser\",\"role\":\"user\",\"roleRequestStatus\":\"none\",\"profilePictureUrl\":null}}"));

        verify(userService).login(email, "password123");
    }

    private String encryptPassword(String password) {
        String key = "heritage_sharing_platform";
        StringBuilder encrypted = new StringBuilder();
        for (int i = 0; i < password.length(); i++) {
            encrypted.append((char) (password.charAt(i) ^ key.charAt(i % key.length())));
        }
        return java.util.Base64.getEncoder().encodeToString(encrypted.toString().getBytes());
    }

    @Test
    @DisplayName("UT-AC-07: login with password returns 401 when credentials are invalid")
    void loginWithPasswordReturnsUnauthorizedWhenInvalid() throws Exception {
        String email = "test@example.com";
        String password = "wrongpassword";

        when(userService.login(email, password)).thenReturn(null);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    @DisplayName("UT-AC-08: login with verification code returns success when valid")
    void loginWithCodeReturnsSuccessWhenValid() throws Exception {
        String email = "test@example.com";
        String code = "1234";

        when(emailVerificationService.verifyCode(email, code)).thenReturn(true);
        when(userService.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(1L, UserRole.USER)).thenReturn("test-token");
        doNothing().when(emailVerificationService).removeCode(email);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("verificationCode", code);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(emailVerificationService).verifyCode(email, code);
        verify(emailVerificationService).removeCode(email);
    }

    @Test
    @DisplayName("UT-AC-09: checkEmail returns success when email exists")
    void checkEmailReturnsSuccessWhenExists() throws Exception {
        String email = "test@example.com";

        when(userService.findByEmail(email)).thenReturn(Optional.of(testUser));

        Map<String, String> request = new HashMap<>();
        request.put("email", email);

        mockMvc.perform(post("/api/auth/check-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Email found"));
    }

    @Test
    @DisplayName("UT-AC-10: checkEmail returns 404 when email does not exist")
    void checkEmailReturnsNotFoundWhenNotExists() throws Exception {
        String email = "nonexistent@example.com";

        when(userService.findByEmail(email)).thenReturn(Optional.empty());

        Map<String, String> request = new HashMap<>();
        request.put("email", email);

        mockMvc.perform(post("/api/auth/check-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Email not found"));
    }

    @Test
    @DisplayName("UT-AC-11: checkEmail returns 403 when email is admin account")
    void checkEmailReturnsForbiddenWhenAdminAccount() throws Exception {
        String email = "admin@example.com";
        User adminUser = new User();
        adminUser.setUserId(2L);
        adminUser.setEmail(email);
        adminUser.setUsername("Admin");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setRoleRequestStatus(com.example.heritage_sharing_api.entity.ContributorRequestStatus.NONE);

        when(userService.findByEmail(email)).thenReturn(Optional.of(adminUser));

        Map<String, String> request = new HashMap<>();
        request.put("email", email);

        mockMvc.perform(post("/api/auth/check-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Admin account is not allowed"));
    }

    @Test
    @DisplayName("UT-AC-12: resetPassword returns success when valid")
    void resetPasswordReturnsSuccessWhenValid() throws Exception {
        String email = "test@example.com";
        String newPassword = "newpassword123";
        String code = "1234";

        when(emailVerificationService.verifyCode(email, code)).thenReturn(true);
        when(userService.resetPassword(email, newPassword)).thenReturn(testUser);
        doNothing().when(emailVerificationService).removeCode(email);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("newPassword", encryptPassword(newPassword));
        request.put("verificationCode", code);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successfully"));

        verify(emailVerificationService).verifyCode(email, code);
        verify(userService).resetPassword(email, newPassword);
        verify(emailVerificationService).removeCode(email);
    }

    @Test
    @DisplayName("UT-AC-13: resetPassword returns 400 when verification code is invalid")
    void resetPasswordReturnsBadRequestWhenCodeInvalid() throws Exception {
        String email = "test@example.com";

        when(emailVerificationService.verifyCode(email, "1234")).thenReturn(false);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("newPassword", "newpassword123");
        request.put("verificationCode", "1234");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid verification code"));

        verify(userService, never()).resetPassword(any(), any());
    }

    @Test
    @DisplayName("UT-AC-14: resetPassword returns 404 when email does not exist")
    void resetPasswordReturnsNotFoundWhenEmailNotExists() throws Exception {
        String email = "nonexistent@example.com";
        String newPassword = "newpassword123";

        when(emailVerificationService.verifyCode(email, "1234")).thenReturn(true);
        when(userService.resetPassword(email, newPassword)).thenReturn(null);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("newPassword", encryptPassword(newPassword));
        request.put("verificationCode", "1234");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Email not found"));
    }

    @Test
    @DisplayName("UT-AC-15: adminLogin returns success when credentials are valid")
    void adminLoginReturnsSuccessWhenValid() throws Exception {
        String email = "admin@example.com";
        String password = "adminpass123";
        User adminUser = new User();
        adminUser.setUserId(2L);
        adminUser.setEmail(email);
        adminUser.setUsername("Admin");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setRoleRequestStatus(com.example.heritage_sharing_api.entity.ContributorRequestStatus.NONE);

        when(userService.login(email, password)).thenReturn(adminUser);
        when(jwtUtil.generateToken(2L, UserRole.ADMIN)).thenReturn("admin-token");

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", encryptPassword(password));

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).login(email, password);
        verify(jwtUtil).generateToken(2L, UserRole.ADMIN);
    }

    @Test
    @DisplayName("UT-AC-16: adminLogin returns 400 when fields are missing")
    void adminLoginReturnsBadRequestWhenFieldsMissing() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "admin@example.com");

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing required fields"));
    }

    @Test
    @DisplayName("UT-AC-17: adminLogin returns 401 when credentials are invalid")
    void adminLoginReturnsUnauthorizedWhenInvalid() throws Exception {
        String email = "admin@example.com";
        String password = "wrongpass";

        when(userService.login(email, password)).thenReturn(null);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", encryptPassword(password));

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    @DisplayName("UT-AC-18: login returns 400 when email is missing")
    void loginReturnsBadRequestWhenEmailMissing() throws Exception {
        Map<String, String> request = new HashMap<>();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing required fields"));
    }

    @Test
    @DisplayName("UT-AC-19: login with verification code returns 401 when user not found")
    void loginWithCodeReturnsUnauthorizedWhenUserNotFound() throws Exception {
        String email = "nonexistent@example.com";
        String code = "1234";

        when(emailVerificationService.verifyCode(email, code)).thenReturn(true);
        when(userService.findByEmail(email)).thenReturn(Optional.empty());

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("verificationCode", code);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or verification code"));
    }

    @Test
    @DisplayName("UT-AC-20: login returns 401 when user role cannot use public login")
    void loginReturnsUnauthorizedWhenRoleNotAllowed() throws Exception {
        String email = "admin@example.com";
        String password = "password123";
        User adminUser = new User();
        adminUser.setUserId(2L);
        adminUser.setEmail(email);
        adminUser.setUsername("Admin");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setRoleRequestStatus(com.example.heritage_sharing_api.entity.ContributorRequestStatus.NONE);

        when(userService.login(email, password)).thenReturn(adminUser);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", encryptPassword(password));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    @DisplayName("UT-AC-21: login returns 400 when neither password nor verification code provided")
    void loginReturnsBadRequestWhenNoAuthMethod() throws Exception {
        String email = "test@example.com";

        Map<String, String> request = new HashMap<>();
        request.put("email", email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing required fields"));
    }

    @Test
    @DisplayName("UT-AC-22: checkEmail returns 400 when email is missing")
    void checkEmailReturnsBadRequestWhenEmailMissing() throws Exception {
        Map<String, String> request = new HashMap<>();

        mockMvc.perform(post("/api/auth/check-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing email"));
    }

    @Test
    @DisplayName("UT-AC-23: register returns 400 when IllegalArgumentException is thrown")
    void registerReturnsBadRequestWhenIllegalArgument() throws Exception {
        String email = "newuser@example.com";
        String password = "password123";
        String username = "NewUser";
        String code = "1234";

        when(emailVerificationService.verifyCode(email, code)).thenReturn(true);
        when(userService.register(email, password, username)).thenThrow(new IllegalArgumentException("Invalid email format"));

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", encryptPassword(password));
        request.put("username", username);
        request.put("verificationCode", code);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid email format"));
    }

    @Test
    @DisplayName("UT-AC-24: register returns 500 when Redis connection fails")
    void registerReturnsInternalServerErrorWhenRedisFails() throws Exception {
        String email = "newuser@example.com";
        String password = "password123";
        String username = "NewUser";
        String code = "1234";

        when(emailVerificationService.verifyCode(email, code)).thenReturn(true);
        when(userService.register(email, password, username)).thenThrow(new RuntimeException("Redis connection failed"));

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", encryptPassword(password));
        request.put("username", username);
        request.put("verificationCode", code);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to connect to Redis: Redis connection failed"));
    }

    @Test
    @DisplayName("UT-AC-25: resetPassword returns 400 when fields are missing")
    void resetPasswordReturnsBadRequestWhenFieldsMissing() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing required fields"));
    }

    @Test
    @DisplayName("UT-AC-26: resetPassword returns 400 when IllegalArgumentException is thrown")
    void resetPasswordReturnsBadRequestWhenIllegalArgument() throws Exception {
        String email = "test@example.com";
        String newPassword = "weak";
        String code = "1234";

        when(emailVerificationService.verifyCode(email, code)).thenReturn(true);
        when(userService.resetPassword(email, newPassword)).thenThrow(new IllegalArgumentException("Password too weak"));

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("newPassword", encryptPassword(newPassword));
        request.put("verificationCode", code);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Password too weak"));
    }

    @Test
    @DisplayName("UT-AC-27: resetPassword returns 500 when Redis connection fails")
    void resetPasswordReturnsInternalServerErrorWhenRedisFails() throws Exception {
        String email = "test@example.com";
        String newPassword = "newpassword123";
        String code = "1234";

        when(emailVerificationService.verifyCode(email, code)).thenReturn(true);
        when(userService.resetPassword(email, newPassword)).thenThrow(new RuntimeException("Redis connection failed"));

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("newPassword", encryptPassword(newPassword));
        request.put("verificationCode", code);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to connect to Redis: Redis connection failed"));
    }
}