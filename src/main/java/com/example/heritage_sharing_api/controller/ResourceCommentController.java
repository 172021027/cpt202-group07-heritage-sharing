package com.example.heritage_sharing_api.controller;

import com.example.heritage_sharing_api.dto.ResourceCommentRequest;
import com.example.heritage_sharing_api.dto.ResourceCommentResponse;
import com.example.heritage_sharing_api.service.ResourceCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@RestController
@RequestMapping("/api/resource-comments")
@CrossOrigin("*")
public class ResourceCommentController {

    @Autowired
    private ResourceCommentService resourceCommentService;

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<ResourceCommentResponse>> getCommentsByResourceId(
            @PathVariable Long resourceId,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isAdmin = isAdmin(authentication);
        try {
            List<ResourceCommentResponse> comments = resourceCommentService.getCommentsByResourceId(resourceId, currentUserId, isAdmin);
            return ResponseEntity.ok(comments);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addComment(@RequestBody ResourceCommentRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in to comment"));
        }
        if (request.getResourceId() == null || request.getCommentText() == null || request.getCommentText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Comment content cannot be empty"));
        }

        try {
            ResourceCommentResponse comment = resourceCommentService.addComment(
                    request.getResourceId(),
                    getCurrentUserId(authentication),
                    request.getCommentText());
            return ResponseEntity.ok(comment);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in to delete comments"));
        }

        try {
            resourceCommentService.deleteComment(commentId, getCurrentUserId(authentication), isAdmin(authentication));
            return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        return Long.valueOf(Objects.toString(principal));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
