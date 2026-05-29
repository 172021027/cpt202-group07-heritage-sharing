package com.example.heritage_sharing_api.controller;

import com.example.heritage_sharing_api.dto.UserResponse;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.security.JwtUtil;
import com.example.heritage_sharing_api.service.EmailVerificationService;
import com.example.heritage_sharing_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailVerificationService emailVerificationService;

    // Decrypt password encrypted by frontend
    private String decryptPassword(String encryptedPassword) {
        try {
            String key = "heritage_sharing_platform";
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPassword);
            StringBuilder decrypted = new StringBuilder();
            for (int i = 0; i < encryptedBytes.length; i++) {
                decrypted.append((char) (encryptedBytes[i] ^ key.charAt(i % key.length())));
            }
            return decrypted.toString();
        } catch (Exception e) {
            // If decryption fails, return original password (might be unencrypted)
            return encryptedPassword;
        }
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getEmail() == null || request.getPassword() == null || request.getUsername() == null || request.getVerificationCode() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        try {
            // Verify verification code
            if (!emailVerificationService.verifyCode(request.getEmail(), request.getVerificationCode())) {
                return ResponseEntity.badRequest().body("Invalid verification code");
            }

            // Decrypt password
            String decryptedPassword = decryptPassword(request.getPassword());
            // Register user
            User user = userService.register(request.getEmail(), decryptedPassword, request.getUsername());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
            }

            // Remove verification code after successful registration
            emailVerificationService.removeCode(request.getEmail());

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getUserId(), user.getRole());

            // Return user information and token
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", UserResponse.from(user));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to connect to Redis: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        try {
            // Check if verification code is provided
            if (request.getVerificationCode() != null) {
                // Verify verification code
                if (!emailVerificationService.verifyCode(request.getEmail(), request.getVerificationCode())) {
                    return ResponseEntity.badRequest().body("Invalid verification code");
                }

                // Find user by email
                User user = userService.findByEmail(request.getEmail()).orElse(null);
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or verification code");
                }

                if (!user.getRole().canUsePublicLogin()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or verification code");
                }

                // Remove verification code after successful login
                emailVerificationService.removeCode(request.getEmail());

                // Generate JWT token
                String token = jwtUtil.generateToken(user.getUserId(), user.getRole());

                // Return user information and token
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("user", UserResponse.from(user));

                return ResponseEntity.ok(response);
            } else if (request.getPassword() != null) {
                // Traditional password login
                String decryptedPassword = decryptPassword(request.getPassword());
                User user = userService.login(request.getEmail(), decryptedPassword);
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
                }

                if (!user.getRole().canUsePublicLogin()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
                }

                // Generate JWT token
                String token = jwtUtil.generateToken(user.getUserId(), user.getRole());

                // Return user information and token
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("user", UserResponse.from(user));

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("Missing required fields");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to connect to Redis: " + e.getMessage());
        }
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        String decryptedPassword = decryptPassword(request.getPassword());
        User user = userService.login(request.getEmail(), decryptedPassword);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        if (!user.getRole().canUseAdminLogin()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole());

        // Return user information and token
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", UserResponse.from(user));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null) {
            return ResponseEntity.badRequest().body("Missing email");
        }

        User user = userService.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found");
        } else if (user.getRole() == UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin account is not allowed");
        } else {
            return ResponseEntity.ok().body("Email found");
        }
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null) {
            return ResponseEntity.badRequest().body("Missing email");
        }

        try {
            String code = emailVerificationService.generateVerificationCode(email);

            Map<String, String> response = new HashMap<>();
            response.put("code", code);
            response.put("message", "Verification code generated successfully");

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to connect to Redis: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getEmail() == null || request.getNewPassword() == null || request.getVerificationCode() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        try {
            // Verify verification code
            if (!emailVerificationService.verifyCode(request.getEmail(), request.getVerificationCode())) {
                return ResponseEntity.badRequest().body("Invalid verification code");
            }

            // Decrypt password
            String decryptedPassword = decryptPassword(request.getNewPassword());
            User user = userService.resetPassword(request.getEmail(), decryptedPassword);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found");
            }

            // Remove verification code after successful password reset
            emailVerificationService.removeCode(request.getEmail());

            return ResponseEntity.ok().body("Password reset successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to connect to Redis: " + e.getMessage());
        }
    }

    // Request payloads
    public static class RegisterRequest {
        private String email;
        private String password;
        private String username;
        private String verificationCode;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getVerificationCode() {
            return verificationCode;
        }

        public void setVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
        }
    }

    public static class LoginRequest {
        private String email;
        private String password;
        private String verificationCode;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getVerificationCode() {
            return verificationCode;
        }

        public void setVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
        }
    }

    public static class ResetPasswordRequest {
        private String email;
        private String newPassword;
        private String verificationCode;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getVerificationCode() {
            return verificationCode;
        }

        public void setVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
        }
    }
}
