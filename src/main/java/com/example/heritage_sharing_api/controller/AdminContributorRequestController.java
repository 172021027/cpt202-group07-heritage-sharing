package com.example.heritage_sharing_api.controller;

import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/contributor-requests")
public class AdminContributorRequestController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPendingRequests() {
        List<Map<String, Object>> requests = userService.getPendingContributorRequests().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/contributors")
    public ResponseEntity<List<Map<String, Object>>> getContributors() {
        List<Map<String, Object>> contributors = userService.getContributors().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(contributors);
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Long userId) {
        User user = userService.approveContributorRequest(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body("Contributor request not found");
        }
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long userId) {
        User user = userService.rejectContributorRequest(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body("Contributor request not found");
        }
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/{userId}/revoke")
    public ResponseEntity<?> revokeContributor(@PathVariable Long userId) {
        User user = userService.revokeContributorAccess(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body("Contributor not found");
        }
        return ResponseEntity.ok(toResponse(user));
    }

    private Map<String, Object> toResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getUserId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRoleValue());
        response.put("roleRequestStatus", user.getRoleRequestStatusValue());
        return response;
    }
}
