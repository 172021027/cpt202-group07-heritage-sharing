package com.example.heritage_sharing_api.controller;

import com.example.heritage_sharing_api.dto.admin.ResourceActionRequestDto;
import com.example.heritage_sharing_api.dto.PublicResourceDto;
import com.example.heritage_sharing_api.dto.ResourceActionResponse;
import com.example.heritage_sharing_api.dto.SubmitResourceRequest;
import com.example.heritage_sharing_api.dto.admin.AdminResourceListItemDto;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceActionType;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.service.ResourceService;
import com.example.heritage_sharing_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin("*")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private UserService userService;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitResource(
            @RequestParam("title") String title,
            @RequestParam("location") String location,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("description") String description,
            @RequestParam("tags") List<String> tags,
            @RequestParam("copyrightDeclaration") String copyrightDeclaration,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "video", required = false) MultipartFile video) {
        Map<String, Object> response = new HashMap<>();
        Long currentUserId;
        try {
            currentUserId = resolveCurrentUserId();
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User currentUser = userService.getUserById(currentUserId).orElse(null);
        if (!userService.isContributor(currentUser)) {
            response.put("success", false);
            response.put("message", "Only Contributor users can submit resources.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            SubmitResourceRequest request = new SubmitResourceRequest();
            request.setTitle(title);
            request.setLocation(location);
            request.setCategoryId(categoryId);
            request.setDescription(description);
            request.setTags(tags);
            request.setCopyrightDeclaration(copyrightDeclaration);
            request.setContributorId(currentUserId);

            Resource savedResource = resourceService.submitResource(request, image, video);
            response.put("success", true);
            response.put("message", "Resource submitted successfully.");
            response.put("resourceId", savedResource.getResourceId());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error uploading files: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error submitting resource: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/mine/{id}")
    public ResponseEntity<Map<String, Object>> deleteOwnResource(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = resourceService.deleteOwnResource(id, resolveCurrentUserId());
            if (!deleted) {
                response.put("success", false);
                response.put("message", "Resource not found.");
                return ResponseEntity.badRequest().body(response);
            }
            response.put("success", true);
            response.put("message", "Resource deleted successfully.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicResourceDto> getResource(@PathVariable Long id) {
        PublicResourceDto resource = resourceService.getApprovedResourceForPublicView(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resource);
    }

    @GetMapping
    public ResponseEntity<List<AdminResourceListItemDto>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @GetMapping("/approved")
    public ResponseEntity<List<PublicResourceDto>> getApprovedResources() {
        return ResponseEntity.ok(resourceService.getApprovedResourcesForPublicView());
    }

    @GetMapping("/search")
    public ResponseEntity<List<PublicResourceDto>> searchResources(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(resourceService.searchResources(keyword));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<PublicResourceDto>> filterResources(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(resourceService.filterResources(categoryId, tag, location));
    }

    @GetMapping("/search-and-filter")
    public ResponseEntity<List<PublicResourceDto>> searchAndFilterResources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(resourceService.searchAndFilterResources(keyword, categoryId, tag, location));
    }

    @GetMapping("/list-frontend")
    public ResponseEntity<List<PublicResourceDto>> getAllResourcesForFrontend() {
        return ResponseEntity.ok(resourceService.getApprovedResourcesForPublicView());
    }

    // Retrieve a paginated action history for administrators.
    @GetMapping("/actions/history")
    public ResponseEntity<Map<String, Object>> getActionsHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Create a paginated request ordered by action time descending
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "actionAt", "actionId"));
            Page<ResourceActionResponse> actionPage = resourceService.getAllResourceActionsWithDetails(pageable);
            
            response.put("success", true);
            response.put("data", actionPage.getContent());
            response.put("totalElements", actionPage.getTotalElements());
            response.put("totalPages", actionPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("hasNext", actionPage.hasNext());
            response.put("hasPrevious", actionPage.hasPrevious());
            response.put("actionTypes", Arrays.stream(ResourceActionType.values())
                    .map(ResourceActionType::name)
                    .toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch action history: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/offline/{id}")
    public ResponseEntity<Map<String, Object>> offlineResource(@PathVariable Long id,
                                                               @RequestBody(required = false) ResourceActionRequestDto request) {
        return handleResourceAction(
                () -> resourceService.offlineResource(id, resolveCurrentUserId(), extractNote(request)),
                "Resource unpublished successfully."
        );
    }

    @PutMapping("/archive/{id}")
    public ResponseEntity<Map<String, Object>> archiveResource(@PathVariable Long id,
                                                               @RequestBody(required = false) ResourceActionRequestDto request) {
        return handleResourceAction(
                () -> resourceService.archiveResource(id, resolveCurrentUserId(), extractNote(request)),
                "Resource archived successfully."
        );
    }

    @PutMapping("/restore/{id}")
    public ResponseEntity<Map<String, Object>> restoreResource(@PathVariable Long id,
                                                               @RequestBody(required = false) ResourceActionRequestDto request) {
        return handleResourceAction(
                () -> resourceService.restoreResource(id, resolveCurrentUserId(), extractNote(request)),
                "Resource restored successfully."
        );
    }

    private ResponseEntity<Map<String, Object>> handleResourceAction(Supplier<Boolean> action, String successMessage) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = action.get();
            if (success) {
                response.put("success", true);
                response.put("message", successMessage);
                return ResponseEntity.ok(response);
            }

            response.put("success", false);
            response.put("message", "Resource not found.");
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process resource action.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private String extractNote(ResourceActionRequestDto request) {
        return request == null ? null : request.getNote();
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String principalString && !principalString.trim().isEmpty()) {
            try {
                return Long.parseLong(principalString.trim());
            } catch (NumberFormatException ignored) {
                // Keep the same user-facing message below.
            }
        }
        throw new IllegalArgumentException("Authenticated user is required.");
    }
}
