package com.example.heritage_sharing_api.controller;

import com.example.heritage_sharing_api.dto.admin.ReviewDecisionRequestDto;
import com.example.heritage_sharing_api.dto.admin.ReviewDecisionResponseDto;
import com.example.heritage_sharing_api.dto.admin.ReviewDetailDto;
import com.example.heritage_sharing_api.dto.admin.ReviewListItemDto;
import com.example.heritage_sharing_api.dto.admin.ReviewListResponseDto;
import com.example.heritage_sharing_api.exception.ReviewConflictException;
import com.example.heritage_sharing_api.exception.ReviewNotFoundException;
import com.example.heritage_sharing_api.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
@CrossOrigin("*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/pending")
    public ResponseEntity<ReviewListResponseDto> getPendingReviews(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        String query = (keyword != null && !keyword.trim().isEmpty()) ? keyword : q;
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? 10 : Math.max(size, 1);

        List<ReviewListItemDto> allItems = reviewService.getPendingReviews(query);
        int totalElements = allItems.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);
        List<ReviewListItemDto> pagedItems = allItems.subList(fromIndex, toIndex);

        ReviewListResponseDto response = new ReviewListResponseDto(
                pagedItems,
                safePage,
                safeSize,
                totalPages,
                totalElements,
                safePage + 1 < totalPages,
                safePage > 0 && totalPages > 0
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<?> getReviewDetail(@PathVariable Long resourceId,
                                             @RequestParam(value = "submissionId", required = false) String submissionId) {
        ReviewDetailDto detailDto = reviewService.getReviewDetail(resourceId);
        if (detailDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detailDto);
    }

    @PostMapping
    public ResponseEntity<?> submitReviewDecision(@RequestBody ReviewDecisionRequestDto request) {
        return submitReviewDecisionInternal(request);
    }

    private ResponseEntity<?> submitReviewDecisionInternal(ReviewDecisionRequestDto request) {
        try {
            request.setActionByUserId(resolveCurrentUserId());
            ReviewDecisionResponseDto responseDto = reviewService.submitReviewDecision(request);
            return ResponseEntity.ok(responseDto);
        } catch (ReviewNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ReviewConflictException e) {
            return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to submit review decision.");
        }
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
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
