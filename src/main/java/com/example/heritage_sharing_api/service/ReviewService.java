package com.example.heritage_sharing_api.service;

import com.example.heritage_sharing_api.dto.admin.ReviewDecisionRequestDto;
import com.example.heritage_sharing_api.dto.admin.ReviewDecisionResponseDto;
import com.example.heritage_sharing_api.dto.admin.ReviewDetailDto;
import com.example.heritage_sharing_api.dto.admin.ReviewListItemDto;
import com.example.heritage_sharing_api.dto.admin.ReviewSubmissionDto;
import com.example.heritage_sharing_api.entity.Category;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceAction;
import com.example.heritage_sharing_api.entity.ResourceActionType;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.Tag;
import com.example.heritage_sharing_api.exception.ReviewConflictException;
import com.example.heritage_sharing_api.exception.ReviewNotFoundException;
import com.example.heritage_sharing_api.repository.CategoryRepository;
import com.example.heritage_sharing_api.repository.ResourceActionRepository;
import com.example.heritage_sharing_api.repository.ResourceRepository;
import com.example.heritage_sharing_api.repository.ResourceTagRepository;
import com.example.heritage_sharing_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final List<ResourceStatus> PENDING_STATUSES = List.of(ResourceStatus.PENDING_REVIEW);
    private static final int MAX_NOTE_LENGTH = 500;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceActionRepository resourceActionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceTagRepository resourceTagRepository;

    public List<ReviewListItemDto> getPendingReviews(String query) {
        List<Resource> resources;
        if (query == null || query.trim().isEmpty()) {
            resources = resourceRepository.findByStatusInOrderBySubmittedAtDesc(PENDING_STATUSES);
        } else {
            resources = resourceRepository.findByStatusInAndTitleContainingIgnoreCaseOrderBySubmittedAtDesc(PENDING_STATUSES, query.trim());
        }

        // Optimized: Fetch only needed categories, not entire table
        // Previous: categoryRepository.findAll() for every call = wasteful full table scan
        // Current: Build map from fetched categories only (still 1 query but at least minimal)
        Set<Long> neededCategoryIds = resources.stream()
                .map(Resource::getCategoryId)
                .collect(Collectors.toSet());
        
        Map<Long, String> categoryNameMap = neededCategoryIds.isEmpty() 
                ? Map.of() 
                : categoryRepository.findAllById(neededCategoryIds).stream()
                        .collect(Collectors.toMap(Category::getCategoryId, Category::getCategoryName, (a, b) -> a));

        return resources.stream()
                .map(resource -> toReviewListItem(resource, categoryNameMap))
                .collect(Collectors.toList());
    }

    public ReviewDetailDto getReviewDetail(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null) {
            return null;
        }

        // Optimized: Fetch only the category needed for this resource
        // Previous: categoryRepository.findAll() returns all categories (wasteful)
        // Current: categoryRepository.findById() returns only needed category (targeted query)
        Map<Long, String> categoryNameMap = resource.getCategoryId() == null 
                ? Map.of() 
                : categoryRepository.findById(resource.getCategoryId())
                        .map(cat -> Map.of(cat.getCategoryId(), cat.getCategoryName()))
                        .orElseGet(Map::of);

        ReviewDetailDto detailDto = new ReviewDetailDto();
        detailDto.setResourceId(resource.getResourceId());
        detailDto.setContributorId(resource.getContributorId());
        detailDto.setContributorUsername(resolveContributorUsername(resource.getContributorId()));
        detailDto.setTitle(resource.getTitle());
        detailDto.setDescription(resource.getDescription());
        detailDto.setCategoryId(resource.getCategoryId());
        detailDto.setCategory(resolveCategory(resource, categoryNameMap));
        detailDto.setLocation(resource.getLocation());
        detailDto.setPicturePath(resource.getPicturePath());
        detailDto.setVideoPath(resource.getVideoPath());
        detailDto.setCopyrightDeclaration(resource.getCopyrightDeclaration());
        detailDto.setStatus(toFrontendStatus(resource.getStatus()));
        detailDto.setSubmittedAt(resource.getSubmittedAt() == null ? "" : resource.getSubmittedAt().format(TIME_FORMATTER));
        detailDto.setApprovedAt(resource.getApprovedAt() == null ? "" : resource.getApprovedAt().format(TIME_FORMATTER));
        detailDto.setTags(resolveTags(resource.getResourceId()));
        return detailDto;
    }

    @Transactional
    public ReviewDecisionResponseDto submitReviewDecision(ReviewDecisionRequestDto request) {
        validateReviewDecisionRequest(request);
        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ReviewNotFoundException("Resource not found."));

        if (resource.getStatus() != ResourceStatus.PENDING_REVIEW) {
            throw new ReviewConflictException("Resource is no longer pending review.");
        }

        String normalizedDecision = request.getDecision().trim().toLowerCase(Locale.ROOT);
        boolean approved = "approve".equals(normalizedDecision);
        LocalDateTime actionTime = LocalDateTime.now();
        ResourceStatus nextStatus = approved ? ResourceStatus.APPROVED : ResourceStatus.REJECTED;
        resource.setStatus(nextStatus);
        resource.setApprovedAt(approved ? actionTime : null);
        Resource savedResource = resourceRepository.save(resource);

        ResourceAction action = new ResourceAction();
        action.setResourceId(savedResource.getResourceId());
        action.setActionType(approved ? ResourceActionType.APPROVE : ResourceActionType.REJECT);
        action.setActionAt(actionTime);
        action.setFeedbackText(request.getNote().trim());
        action.setActionByUserId(request.getActionByUserId());
        ResourceAction savedAction = resourceActionRepository.save(action);

        ReviewDecisionResponseDto responseDto = new ReviewDecisionResponseDto();
        responseDto.setSuccess(true);
        responseDto.setResourceId(savedResource.getResourceId());
        responseDto.setSubmissionId(request.getSubmissionId());
        responseDto.setStatus(toFrontendStatus(savedResource.getStatus()));
        responseDto.setActionId(savedAction.getActionId());
        responseDto.setActionAt(savedAction.getActionAt().format(TIME_FORMATTER));
        responseDto.setMessage("Review decision recorded successfully.");
        return responseDto;
    }

    private void validateReviewDecisionRequest(ReviewDecisionRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getResourceId() == null) {
            throw new IllegalArgumentException("resourceId is required.");
        }
        if (request.getDecision() == null || request.getDecision().trim().isEmpty()) {
            throw new IllegalArgumentException("decision is required.");
        }
        String decision = request.getDecision().trim().toLowerCase(Locale.ROOT);
        if (!Objects.equals(decision, "approve") && !Objects.equals(decision, "reject")) {
            throw new IllegalArgumentException("decision must be approve or reject.");
        }
        if (request.getNote() == null || request.getNote().trim().isEmpty()) {
            throw new IllegalArgumentException("note is required.");
        }
        if (request.getNote().trim().length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("note must be 500 characters or fewer.");
        }
        if (request.getActionByUserId() == null) {
            throw new IllegalArgumentException("actionByUserId is required.");
        }
        if (!userRepository.existsById(request.getActionByUserId())) {
            throw new IllegalArgumentException("Reviewer account not found.");
        }
    }

    private ReviewListItemDto toReviewListItem(Resource resource, Map<Long, String> categoryNameMap) {
        ReviewListItemDto itemDto = new ReviewListItemDto();
        itemDto.setResourceId(resource.getResourceId());
        itemDto.setTitle(resource.getTitle());
        itemDto.setCategory(resolveCategory(resource, categoryNameMap));
        itemDto.setStatus(toFrontendStatus(resource.getStatus()));
        itemDto.setSubmissions(Collections.singletonList(toSubmission(resource)));
        return itemDto;
    }

    private ReviewSubmissionDto toSubmission(Resource resource) {
        ReviewSubmissionDto submissionDto = new ReviewSubmissionDto();
        submissionDto.setSubmissionId("resource-" + resource.getResourceId());
        submissionDto.setSubmittedAt(resource.getSubmittedAt() == null ? "" : resource.getSubmittedAt().format(TIME_FORMATTER));
        submissionDto.setSummary(resource.getDescription());
        submissionDto.setContent(resource.getDescription());
        submissionDto.setPicture(resource.getPicturePath());
        submissionDto.setVideo(resource.getVideoPath());
        return submissionDto;
    }

    private String resolveCategory(Resource resource, Map<Long, String> categoryNameMap) {
        if (resource.getCategoryId() == null) {
            return "unknown";
        }
        String category = categoryNameMap.get(resource.getCategoryId());
        return category == null ? String.valueOf(resource.getCategoryId()) : category;
    }

    private String toFrontendStatus(ResourceStatus status) {
        if (status == null) {
            return "pending_review";
        }
        return status.getFrontendValue();
    }

    private String resolveContributorUsername(Long contributorId) {
        if (contributorId == null) {
            return "unknown";
        }
        return userRepository.findById(contributorId)
                .map(user -> user.getUsername() == null || user.getUsername().trim().isEmpty()
                        ? String.valueOf(contributorId)
                        : user.getUsername())
                .orElse(String.valueOf(contributorId));
    }

    private List<ReviewDetailDto.ReviewTagDto> resolveTags(Long resourceId) {
        if (resourceId == null) {
            return Collections.emptyList();
        }

        // Optimized: Use JOIN query to fetch tags directly instead of two separate queries
        // Previous approach: findByResourceId (query 1) + findAllById (query 2) = 2 queries
        // Current approach: Single JOIN query combining both lookups = 1 query (50% improvement)
        List<Tag> tags = resourceTagRepository.findTagsByResourceId(resourceId);
        
        return tags.stream()
                .map(this::toReviewTagDto)
                .collect(Collectors.toList());
    }

    private ReviewDetailDto.ReviewTagDto toReviewTagDto(Tag tag) {
        ReviewDetailDto.ReviewTagDto tagDto = new ReviewDetailDto.ReviewTagDto();
        tagDto.setTagId(tag.getTagId());
        tagDto.setTagName(tag.getTagName());
        return tagDto;
    }
}
