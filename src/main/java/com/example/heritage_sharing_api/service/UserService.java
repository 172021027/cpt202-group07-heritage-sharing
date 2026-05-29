package com.example.heritage_sharing_api.service;

import com.example.heritage_sharing_api.dto.UserStatsResponse;
import com.example.heritage_sharing_api.entity.ContributorRequestStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.repository.ResourceRepository;
import com.example.heritage_sharing_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final String AVATAR_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "avatars" + File.separator;
    private static final String AVATAR_URL_PREFIX = "/uploads/avatars/";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            Long userId = (Long) principal;
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User register(String email, String password, String username) {
        // Check whether the email is already registered.
        if (userRepository.findByEmail(email).isPresent()) {
            return null;
        }

        // Validate password length.
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Create a regular user by default.
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUsername(username);
        user.setRole(UserRole.USER);
        user.setRoleRequestStatus(ContributorRequestStatus.NONE);

        return userRepository.save(user);
    }

    public boolean isContributor(User user) {
        return user != null && user.getRole() == UserRole.CONTRIBUTOR;
    }

    public boolean isRegularUser(User user) {
        return user != null && user.getRole() == UserRole.USER;
    }

    @Transactional
    public User requestContributorRole(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalStateException("User not found");
        }
        if (!isRegularUser(user)) {
            throw new IllegalStateException("Only User accounts can request Contributor access");
        }
        ContributorRequestStatus currentStatus = user.getRoleRequestStatus();
        if (currentStatus == ContributorRequestStatus.NONE
                || currentStatus == ContributorRequestStatus.REJECTED
                || currentStatus == ContributorRequestStatus.REVOKED) {
            user.setRoleRequestStatus(ContributorRequestStatus.PENDING);
            return userRepository.save(user);
        }
        if (currentStatus == ContributorRequestStatus.PENDING) {
            throw new IllegalStateException("Your contributor request is already pending admin approval.");
        }
        if (currentStatus == ContributorRequestStatus.APPROVED) {
            throw new IllegalStateException("Your contributor request has already been approved.");
        }
        throw new IllegalStateException("Cannot submit contributor request in current state.");
    }

    public List<User> getPendingContributorRequests() {
        return userRepository.findByRoleRequestStatusOrderByUserIdAsc(ContributorRequestStatus.PENDING);
    }

    public List<User> getContributors() {
        return userRepository.findByRoleOrderByUserIdAsc(UserRole.CONTRIBUTOR);
    }

    public User approveContributorRequest(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleRequestStatus() != ContributorRequestStatus.PENDING) {
            return null;
        }
        user.setRole(UserRole.CONTRIBUTOR);
        user.setRoleRequestStatus(ContributorRequestStatus.APPROVED);
        return userRepository.save(user);
    }

    public User rejectContributorRequest(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleRequestStatus() != ContributorRequestStatus.PENDING) {
            return null;
        }
        user.setRole(UserRole.USER);
        user.setRoleRequestStatus(ContributorRequestStatus.REJECTED);
        return userRepository.save(user);
    }

    public User revokeContributorAccess(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() != UserRole.CONTRIBUTOR) {
            return null;
        }
        user.setRole(UserRole.USER);
        user.setRoleRequestStatus(ContributorRequestStatus.REVOKED);
        return userRepository.save(user);
    }

    public User login(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                return user;
            }
        }
        return null;
    }

    public User updateProfile(Long userId, String username, String gender, String personalDescription) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        if (username != null) {
            user.setUsername(username);
        }
        if (gender != null) {
            user.setGender(normalizeGender(gender));
        }
        if (personalDescription != null) {
            user.setPersonalDescription(personalDescription);
        }
        return userRepository.save(user);
    }

    private String normalizeGender(String gender) {
        if (gender.isBlank()) {
            return null;
        }
        if ("Male".equals(gender) || "Female".equals(gender)) {
            return gender;
        }
        return null;
    }

    public String saveAvatar(Long userId, MultipartFile avatarFile) throws IOException {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return null;
        }

        File avatarDir = new File(AVATAR_DIR);
        if (!avatarDir.exists()) {
            boolean created = avatarDir.mkdirs();
            if (!created && !avatarDir.exists()) {
                throw new IOException("Failed to create avatar upload directory: " + AVATAR_DIR);
            }
        }

        String originalFilename = avatarFile.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String fileName = System.currentTimeMillis() + (extension.isEmpty() ? ".png" : extension);
        File targetFile = new File(AVATAR_DIR + fileName);
        avatarFile.transferTo(targetFile);

        String relativePath = AVATAR_URL_PREFIX + fileName;
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setProfilePicturePath(relativePath);
            userRepository.save(user);
        }
        return relativePath;
    }

    public UserStatsResponse getUserStats(Long userId) {
        // Optimized: Fetch all status counts in a single aggregation query
        // Previous approach: Three separate count queries for PENDING_REVIEW, APPROVED, REJECTED = 3 database calls
        // Current approach: Single COUNT with CASE WHEN aggregation = 1 database call (66% reduction)
        Object[] counts = resourceRepository.countByContributorIdAndMultipleStatuses(userId);
        
        long pending = counts[0] != null ? ((Number) counts[0]).longValue() : 0;
        long approved = counts[1] != null ? ((Number) counts[1]).longValue() : 0;
        long rejected = counts[2] != null ? ((Number) counts[2]).longValue() : 0;
        
        return new UserStatsResponse(pending, approved, rejected);
    }

    public User resetPassword(String email, String newPassword) {
        // Validate password length.
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            return userRepository.save(user);
        }
        return null;
    }

}
