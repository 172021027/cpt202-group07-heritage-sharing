package com.example.heritage_sharing_api.controller;

import com.example.heritage_sharing_api.dto.RejectedSubmissionEditResponse;
import com.example.heritage_sharing_api.dto.SubmitResourceRequest;
import com.example.heritage_sharing_api.dto.UserResponse;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.service.ResourceService;
import com.example.heritage_sharing_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private ResourceService resourceService;

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/current")
    public ResponseEntity<?> updateCurrentUser(@RequestBody UpdateUserRequest request) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        User updatedUser = userService.updateProfile(
                user.getUserId(),
                request.getUsername(),
                request.getGender(),
                request.getPersonalDescription()
        );

        if (updatedUser == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update profile");
        }

        return ResponseEntity.ok("Profile updated successfully");
    }

    @PostMapping("/current/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile avatarFile) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        try {
            String avatarUrl = userService.saveAvatar(user.getUserId(), avatarFile);
            if (avatarUrl == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload avatar");
            }

            Map<String, String> response = new HashMap<>();
            response.put("profilePictureUrl", avatarUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload avatar: " + e.getMessage());
        }
    }

    @PostMapping("/current/contributor-request")
    public ResponseEntity<?> requestContributorRole() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        try {
            User updatedUser = userService.requestContributorRole(user.getUserId());
            Map<String, Object> response = new HashMap<>();
            response.put("role", updatedUser.getRoleValue());
            response.put("roleRequestStatus", updatedUser.getRoleRequestStatusValue());
            response.put("message", "Contributor request submitted.");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            String message = e.getMessage() == null ? "Cannot submit contributor request." : e.getMessage();
            HttpStatus status = message.contains("already") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(message);
        }
    }

    @GetMapping("/current/submissions")
    public ResponseEntity<?> getCurrentUserSubmissions() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        return ResponseEntity.ok(resourceService.getContributorSubmissions(user.getUserId()));
    }

    @GetMapping("/current/submissions/{resourceId}")
    public ResponseEntity<?> getCurrentUserSubmissionForEdit(@PathVariable Long resourceId) {
        User user = userService.getCurrentUser();
        ResponseEntity<?> contributorError = validateContributorAccess(user, "view submissions");
        if (contributorError != null) {
            return contributorError;
        }

        try {
            RejectedSubmissionEditResponse response = resourceService.getRejectedSubmissionForEdit(
                    user.getUserId(),
                    resourceId
            );
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found.");
        } catch (IllegalStateException e) {
            return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/current/submissions/{resourceId}/resubmit")
    public ResponseEntity<?> resubmitCurrentUserSubmission(
            @PathVariable Long resourceId,
            @RequestParam("title") String title,
            @RequestParam("location") String location,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("description") String description,
            @RequestParam("tags") List<String> tags,
            @RequestParam("copyrightDeclaration") String copyrightDeclaration,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "video", required = false) MultipartFile video) {
        User user = userService.getCurrentUser();
        ResponseEntity<?> contributorError = validateContributorAccess(user, "resubmit resources");
        if (contributorError != null) {
            return contributorError;
        }

        try {
            SubmitResourceRequest request = new SubmitResourceRequest();
            request.setTitle(title);
            request.setLocation(location);
            request.setCategoryId(categoryId);
            request.setDescription(description);
            request.setTags(tags);
            request.setCopyrightDeclaration(copyrightDeclaration);
            request.setContributorId(user.getUserId());

            Resource savedResource = resourceService.resubmitRejectedSubmission(
                    user.getUserId(),
                    resourceId,
                    request,
                    image,
                    video
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Resource resubmitted for review.");
            response.put("resourceId", savedResource.getResourceId());
            response.put("status", savedResource.getStatus() == null ? null : savedResource.getStatus().getFrontendValue());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found.");
        } catch (IllegalStateException e) {
            return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException | IOException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private ResponseEntity<?> validateContributorAccess(User user, String action) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        if (!userService.isContributor(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only Contributor users can " + action);
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    // Request payloads
    public static class UpdateUserRequest {
        private String username;
        private String gender;
        private String personalDescription;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getGender() { return gender; }
        public String getPersonalDescription() { return personalDescription; }
    }
}
