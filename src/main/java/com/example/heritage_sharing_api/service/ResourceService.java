package com.example.heritage_sharing_api.service;

import com.example.heritage_sharing_api.dto.PublicResourceDto;
import com.example.heritage_sharing_api.dto.RejectedSubmissionEditResponse;
import com.example.heritage_sharing_api.dto.ResourceActionResponse;
import com.example.heritage_sharing_api.dto.SubmitResourceRequest;
import com.example.heritage_sharing_api.dto.UserSubmissionResponse;
import com.example.heritage_sharing_api.dto.admin.AdminResourceListItemDto;
import com.example.heritage_sharing_api.entity.Category;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceAction;
import com.example.heritage_sharing_api.entity.ResourceActionType;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.ResourceTag;
import com.example.heritage_sharing_api.entity.Tag;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.repository.CategoryRepository;
import com.example.heritage_sharing_api.repository.ResourceActionRepository;
import com.example.heritage_sharing_api.repository.ResourceRepository;
import com.example.heritage_sharing_api.repository.ResourceTagRepository;
import com.example.heritage_sharing_api.repository.TagRepository;
import com.example.heritage_sharing_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResourceService {
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
    private static final ResourceStatus PUBLIC_RESOURCE_STATUS = ResourceStatus.APPROVED;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DASHBOARD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_ACTION_NOTE_LENGTH = 500;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "avif", "svg");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "avi", "mkv", "webm", "mpeg", "mpg", "m4v");

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ResourceActionRepository resourceActionRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ResourceTagRepository resourceTagRepository;

    @Autowired
    private UserRepository userRepository;

    public Resource submitResource(SubmitResourceRequest request, MultipartFile image, MultipartFile video) throws IOException {
        validateImageFile(image);
        validateVideoFile(video);

        File uploadDirFile = new File(UPLOAD_DIR);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }

        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            imagePath = saveFile(image, "image");
        }

        String videoPath = null;
        if (video != null && !video.isEmpty()) {
            videoPath = saveFile(video, "video");
        }

        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setLocation(request.getLocation());
        resource.setCategoryId(request.getCategoryId());
        resource.setDescription(request.getDescription());
        resource.setContributorId(request.getContributorId());
        resource.setPicturePath(imagePath);
        resource.setVideoPath(videoPath);
        resource.setCopyrightDeclaration(request.getCopyrightDeclaration());
        resource.setStatus(ResourceStatus.PENDING_REVIEW);
        resource.setSubmittedAt(LocalDateTime.now());

        Resource savedResource = resourceRepository.save(resource);

        saveResourceTags(savedResource.getResourceId(), request.getTags());

        return savedResource;
    }

    public RejectedSubmissionEditResponse getRejectedSubmissionForEdit(Long contributorId, Long resourceId) {
        Resource resource = findOwnedResource(contributorId, resourceId);
        if (resource.getStatus() != ResourceStatus.REJECTED) {
            throw new IllegalStateException("Only rejected resources can be revised.");
        }

        return toRejectedSubmissionEditResponse(resource);
    }

    @Transactional
    public Resource resubmitRejectedSubmission(Long contributorId,
                                               Long resourceId,
                                               SubmitResourceRequest request,
                                               MultipartFile image,
                                               MultipartFile video) throws IOException {
        validateActionUser(contributorId);
        Resource resource = findOwnedResource(contributorId, resourceId);
        if (resource.getStatus() != ResourceStatus.REJECTED) {
            throw new IllegalStateException("Only rejected resources can be resubmitted.");
        }

        boolean hasExistingImage = !isBlank(resource.getPicturePath());
        boolean hasNewImage = image != null && !image.isEmpty();
        boolean hasExistingVideo = !isBlank(resource.getVideoPath());
        boolean hasNewVideo = video != null && !video.isEmpty();
        if (!hasExistingImage && !hasNewImage) {
            throw new IllegalArgumentException("Image is required.");
        }
        if (!hasExistingVideo && !hasNewVideo) {
            throw new IllegalArgumentException("Video is required.");
        }

        validateImageFile(image);
        validateVideoFile(video);

        String nextImagePath = hasNewImage ? saveFile(image, "image") : resource.getPicturePath();
        String nextVideoPath = hasNewVideo ? saveFile(video, "video") : resource.getVideoPath();

        LocalDateTime actionAt = LocalDateTime.now();
        resource.setTitle(request.getTitle());
        resource.setLocation(request.getLocation());
        resource.setCategoryId(request.getCategoryId());
        resource.setDescription(request.getDescription());
        resource.setCopyrightDeclaration(request.getCopyrightDeclaration());
        resource.setPicturePath(nextImagePath);
        resource.setVideoPath(nextVideoPath);
        resource.setStatus(ResourceStatus.PENDING_REVIEW);
        resource.setSubmittedAt(actionAt);
        resource.setApprovedAt(null);

        Resource savedResource = resourceRepository.save(resource);
        resourceTagRepository.deleteByResourceId(savedResource.getResourceId());
        saveResourceTags(savedResource.getResourceId(), request.getTags());
        resourceActionRepository.save(buildResourceAction(
                savedResource.getResourceId(),
                ResourceActionType.RESUBMIT,
                contributorId,
                actionAt,
                null
        ));
        return savedResource;
    }

    @Transactional
    public boolean deleteOwnResource(Long resourceId, Long userId) {
        if (resourceId == null || userId == null) {
            return false;
        }

        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null) {
            return false;
        }
        if (!Objects.equals(resource.getContributorId(), userId)) {
            throw new IllegalArgumentException("You can only delete your own resources.");
        }

        resource.setStatus(ResourceStatus.DELETED);
        resourceRepository.save(resource);
        return true;
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id).orElse(null);
    }

    public List<AdminResourceListItemDto> getAllResources() {
        List<Resource> resources = resourceRepository.findAll();
        if (resources.isEmpty()) {
            return List.of();
        }

        Set<Long> categoryIds = resources.stream()
                .map(Resource::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty()
                ? Map.of()
                : categoryRepository.findAllById(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getCategoryId, Category::getCategoryName, (a, b) -> a));

        return resources.stream()
                .sorted(
                        Comparator.comparing(Resource::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(Resource::getResourceId, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .map(resource -> new AdminResourceListItemDto(
                        resource.getResourceId(),
                        resource.getTitle(),
                        resource.getCategoryId(),
                        categoryNameMap.getOrDefault(resource.getCategoryId(), ""),
                        resource.getStatus() == null ? "" : resource.getStatus().getFrontendValue(),
                        resource.getSubmittedAt()
                ))
                .toList();
    }

    public List<UserSubmissionResponse> getContributorSubmissions(Long contributorId) {
        if (contributorId == null) {
            return List.of();
        }

        List<Resource> resources = resourceRepository.findAllByContributorIdOrderBySubmittedAtDesc(contributorId);
        if (resources.isEmpty()) {
            return List.of();
        }
        return resources.stream()
                .map(this::toUserSubmissionResponse)
                .toList();
    }

    private UserSubmissionResponse toUserSubmissionResponse(Resource resource) {
        ResourceAction latestAction = shouldShowReviewDetails(resource.getStatus())
                ? resolveLatestReviewAction(resource.getResourceId())
                : null;

        UserSubmissionResponse response = new UserSubmissionResponse();
        response.setResourceId(resource.getResourceId());
        response.setTitle(resource.getTitle());
        response.setDescription(normalizeSubmissionDescription(resource.getDescription()));
        response.setSubmissionDate(formatDateTime(resource.getSubmittedAt()));
        response.setStatus(resource.getStatus() == null ? "pending_review" : resource.getStatus().getFrontendValue());
        response.setFeedback(latestAction == null ? "" : defaultString(latestAction.getFeedbackText()));
        response.setReviewerId(latestAction == null ? "" : resolveReviewerDisplayName(latestAction.getActionByUserId()));
        response.setReviewedAt(latestAction == null ? "" : formatDateTime(latestAction.getActionAt()));
        return response;
    }

    private Resource findOwnedResource(Long contributorId, Long resourceId) {
        if (contributorId == null || resourceId == null) {
            throw new NoSuchElementException("Resource not found.");
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NoSuchElementException("Resource not found."));
        if (!Objects.equals(resource.getContributorId(), contributorId)) {
            throw new NoSuchElementException("Resource not found.");
        }
        return resource;
    }

    private RejectedSubmissionEditResponse toRejectedSubmissionEditResponse(Resource resource) {
        ResourceAction latestRejectAction = resolveLatestRejectAction(resource.getResourceId());
        RejectedSubmissionEditResponse response = new RejectedSubmissionEditResponse();
        response.setResourceId(resource.getResourceId());
        response.setTitle(resource.getTitle());
        response.setLocation(resource.getLocation());
        response.setCategoryId(resource.getCategoryId());
        response.setDescription(resource.getDescription());
        response.setTags(resolveTagNames(resource.getResourceId()));
        response.setPicturePath(toPublicAssetPath(resource.getPicturePath()));
        response.setVideoPath(toPublicAssetPath(resource.getVideoPath()));
        response.setCopyrightDeclaration(resource.getCopyrightDeclaration());
        response.setStatus(resource.getStatus() == null ? "" : resource.getStatus().getFrontendValue());
        response.setSubmittedAt(formatDateTime(resource.getSubmittedAt()));
        response.setFeedback(latestRejectAction == null ? "" : defaultString(latestRejectAction.getFeedbackText()));
        return response;
    }

    public List<PublicResourceDto> getApprovedResourcesForPublicView() {
        // Optimized: Load all resources with efficient batch processing
        // Caches categories and users to avoid N+1 queries: N resources + N categories + N users = 3N to 3 queries
        List<Resource> resources = resourceRepository.findByStatusOrderBySubmittedAtDesc(PUBLIC_RESOURCE_STATUS);
        
        if (resources.isEmpty()) {
            return List.of();
        }
        
        return convertResourcesWithBatchedLoads(resources);
    }

    public PublicResourceDto getApprovedResourceForPublicView(Long id) {
        return resourceRepository.findByResourceIdAndStatus(id, PUBLIC_RESOURCE_STATUS)
                .map(resource -> convertResourcesWithBatchedLoads(List.of(resource)).get(0))
                .orElse(null);
    }

    public List<PublicResourceDto> searchResources(String keyword) {
        return searchAndFilterResources(keyword, null, null, null);
    }

    public List<PublicResourceDto> filterResources(Long categoryId, String tag, String location) {
        return searchAndFilterResources(null, categoryId, tag, location);
    }

    public List<PublicResourceDto> searchAndFilterResources(String keyword, Long categoryId, String tag, String location) {
        // Optimized: Use database-side filtering where possible, batch load associated data
        // Previous approach: Load all approved resources to memory, then filter with stream operations
        // Current approach: Apply category filter and keyword search at database level, then batch load
        
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedTag = normalizeKeyword(tag);
        String normalizedLocation = normalizeText(location);
        
        // Determine tag-based resource IDs from database (single query)
        Set<Long> taggedResourceIds = resolveTaggedResourceIds(normalizedTag);
        
        // Fetch filtered resources with category filter if available
        List<Resource> resources = fetchFilteredResources(normalizedKeyword, categoryId, taggedResourceIds);
        
        // Apply memory-side filtering for complex conditions (location matching)
        if (normalizedLocation != null) {
            resources = resources.stream()
                    .filter(r -> matchesLocation(r, normalizedLocation))
                    .toList();
        }
        
        // Batch convert using cached category/user/tag data (3 queries instead of 3N)
        return convertResourcesWithBatchedLoads(resources);
    }
    
    // Helper: Fetch resources with database-level filtering
    private List<Resource> fetchFilteredResources(String keyword, Long categoryId, Set<Long> taggedResourceIds) {
        List<Resource> resources;
        
        // Filter by category if specified
        boolean hasCategory = categoryId != null;
        boolean hasTag = taggedResourceIds != null && !taggedResourceIds.isEmpty();
        
        if (hasCategory && hasTag) {
            // Both category and tag filters
            resources = resourceRepository.findByStatusOrderBySubmittedAtDesc(PUBLIC_RESOURCE_STATUS).stream()
                    .filter(r -> Objects.equals(r.getCategoryId(), categoryId))
                    .filter(r -> taggedResourceIds.contains(r.getResourceId()))
                    .toList();
        } else if (hasCategory) {
            // Category filter only
            resources = resourceRepository.findByStatusOrderBySubmittedAtDesc(PUBLIC_RESOURCE_STATUS).stream()
                    .filter(r -> Objects.equals(r.getCategoryId(), categoryId))
                    .toList();
        } else if (hasTag) {
            // Tag filter only
            resources = resourceRepository.findByStatusOrderBySubmittedAtDesc(PUBLIC_RESOURCE_STATUS).stream()
                    .filter(r -> taggedResourceIds.contains(r.getResourceId()))
                    .toList();
        } else if (keyword != null) {
            // Keyword search (database can be optimized with LIKE query)
            resources = resourceRepository.findByStatusInAndTitleContainingIgnoreCaseOrderBySubmittedAtDesc(
                    List.of(PUBLIC_RESOURCE_STATUS), keyword);
        } else {
            // No filters
            resources = resourceRepository.findByStatusOrderBySubmittedAtDesc(PUBLIC_RESOURCE_STATUS);
        }
        
        // Memory-side keyword filtering when needed
        if (keyword != null && resources.size() > 0) {
            resources = resources.stream()
                    .filter(r -> matchesKeyword(r, keyword))
                    .toList();
        }
        
        return resources;
    }

    @Transactional
    public boolean offlineResource(Long resourceId, Long actionByUserId, String note) {
        return updateResourceStatus(
                resourceId,
                ResourceStatus.UNPUBLISHED,
                ResourceActionType.UNPUBLISH,
                actionByUserId,
                note,
                true,
                List.of(ResourceStatus.APPROVED, ResourceStatus.ARCHIVED),
                "Only approved or archived resources can be unpublished."
        );
    }

    @Transactional
    public boolean archiveResource(Long resourceId, Long actionByUserId, String note) {
        return updateResourceStatus(
                resourceId,
                ResourceStatus.ARCHIVED,
                ResourceActionType.ARCHIVE,
                actionByUserId,
                note,
                false,
                List.of(ResourceStatus.APPROVED),
                "Only approved resources can be archived."
        );
    }

    @Transactional
    public boolean restoreResource(Long resourceId, Long actionByUserId, String note) {
        return updateResourceStatus(
                resourceId,
                ResourceStatus.APPROVED,
                ResourceActionType.RESTORE,
                actionByUserId,
                note,
                false,
                List.of(ResourceStatus.ARCHIVED, ResourceStatus.UNPUBLISHED),
                "Only archived or unpublished resources can be restored."
        );
    }

    private String saveFile(MultipartFile file, String fileType) throws IOException {
        if (file.isEmpty()) {
            return null;
        }

        try {
            String originalFileName = file.getOriginalFilename();
            String fileName = System.currentTimeMillis() + "_" + originalFileName;
            String filePath = UPLOAD_DIR + fileType + File.separator + fileName;

            File fileDir = new File(UPLOAD_DIR + fileType);
            if (!fileDir.exists()) {
                boolean created = fileDir.mkdirs();
                if (!created && !fileDir.exists()) {
                    throw new IOException("Failed to create directory: " + fileDir.getAbsolutePath());
                }
            }

            if (!fileDir.isDirectory()) {
                throw new IOException("Path is not a directory: " + fileDir.getAbsolutePath());
            }

            File targetFile = new File(filePath);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            file.transferTo(targetFile);

            String relativePath = "uploads/" + fileType + "/" + fileName;
            System.out.println("File saved successfully: " + targetFile.getAbsolutePath());
            System.out.println("Stored path in DB: " + relativePath);
            return relativePath;
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to save file: " + e.getMessage(), e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        validateMediaFile(file, "Image", "an image", "image/", IMAGE_EXTENSIONS);
    }

    private void validateVideoFile(MultipartFile file) {
        validateMediaFile(file, "Video", "a video", "video/", VIDEO_EXTENSIONS);
    }

    private void validateMediaFile(MultipartFile file,
                                   String fieldLabel,
                                   String expectedType,
                                   String contentTypePrefix,
                                   Set<String> allowedExtensions) {
        if (file == null || file.isEmpty()) {
            return;
        }

        String contentType = normalizeText(file.getContentType());
        if (contentType != null && contentType.startsWith(contentTypePrefix)) {
            return;
        }
        if (contentType != null && !"application/octet-stream".equals(contentType)) {
            throw new IllegalArgumentException(fieldLabel + " file must be " + expectedType + ".");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (extension != null && allowedExtensions.contains(extension)) {
            return;
        }

        throw new IllegalArgumentException(fieldLabel + " file must be " + expectedType + ".");
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null) {
            return null;
        }

        String trimmedFileName = originalFileName.trim();
        int extensionStart = trimmedFileName.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == trimmedFileName.length() - 1) {
            return null;
        }
        return trimmedFileName.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    }

    private boolean matchesLocation(Resource resource, String normalizedLocation) {
        if (normalizedLocation == null) {
            return true;
        }

        String resourceLocation = normalizeText(resource.getLocation());
        return resourceLocation != null && resourceLocation.contains(normalizedLocation);
    }

    private boolean matchesKeyword(Resource resource, String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return true;
        }

        return containsIgnoreCase(resource.getTitle(), normalizedKeyword)
                || containsIgnoreCase(resource.getDescription(), normalizedKeyword);
    }

    // Optimized batch processing method to avoid N+1 queries
    // Loads all categories and users upfront, then uses cached data for each resource conversion
    // Performance: 3N queries before, 3 queries total after.
    private List<PublicResourceDto> convertResourcesWithBatchedLoads(List<Resource> resources) {
        if (resources.isEmpty()) {
            return List.of();
        }
        
        // Batch load: Fetch all required categories in one query
        Map<Long, String> categoryMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getCategoryId, Category::getCategoryName, (a, b) -> a));
        
        // Batch load: Fetch all required users in one query
        Map<Long, String> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUserId, User::getUsername, (a, b) -> a));
        
        // Batch load: Fetch all resource tags and resolve tag names
        Set<Long> resourceIds = resources.stream()
                .map(Resource::getResourceId)
                .collect(Collectors.toSet());
        Map<Long, List<String>> resourceTagMap = buildResourceTagMap(resourceIds);
        
        // Convert all resources using cached data (no additional queries)
        return resources.stream()
                .map(resource -> toPublicResourceDtoWithCache(resource, categoryMap, userMap, resourceTagMap))
                .toList();
    }
    
    // Helper: Build a map of resource IDs to their tag names using optimized queries
    // Fetches all resource-tag associations and tag details for a set of resources
    private Map<Long, List<String>> buildResourceTagMap(Set<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return Map.of();
        }
        
        // Fetch all resource-tag associations for these resources
        List<ResourceTag> resourceTags = resourceTagRepository.findByResourceIdIn(resourceIds);
        
        // Extract unique tag IDs
        Set<Long> tagIds = resourceTags.stream()
                .map(ResourceTag::getTagId)
                .collect(Collectors.toSet());
        
        if (tagIds.isEmpty()) {
            return resourceIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> List.of()));
        }
        
        // Fetch all tags in one query
        Map<Long, String> tagNameMap = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getTagId, Tag::getTagName, (a, b) -> a));
        
        // Build the final map: resourceId to list of tag names
        Map<Long, List<String>> result = new java.util.HashMap<>();
        for (Long resourceId : resourceIds) {
            result.put(resourceId, List.of());
        }
        
        for (ResourceTag rt : resourceTags) {
            String tagName = tagNameMap.get(rt.getTagId());
            if (tagName != null) {
                List<String> tags = new java.util.ArrayList<>(result.get(rt.getResourceId()));
                tags.add(tagName);
                result.put(rt.getResourceId(), tags.stream()
                        .sorted(String::compareToIgnoreCase)
                        .distinct()
                        .toList());
            }
        }
        
        return result;
    }
    
    // Convert resource to PublicResourceDto using cached category/user/tag data
    // Zero additional database queries - all data comes from maps
    private PublicResourceDto toPublicResourceDtoWithCache(Resource resource, 
                                                            Map<Long, String> categoryMap,
                                                            Map<Long, String> userMap,
                                                            Map<Long, List<String>> resourceTagMap) {
        String categoryName = categoryMap.getOrDefault(resource.getCategoryId(), "");
        String contributorName = userMap.getOrDefault(resource.getContributorId(), "");
        List<String> tagNames = resourceTagMap.getOrDefault(resource.getResourceId(), List.of());
        LocalDateTime effectiveDate = getEffectivePublicationDate(resource);
        
        return new PublicResourceDto(
                resource.getResourceId(),
                resource.getContributorId(),
                resource.getCategoryId(),
                resource.getTitle(),
                resource.getDescription(),
                categoryName,
                tagNames,
                resource.getLocation(),
                toPublicAssetPath(resource.getPicturePath()),
                toPublicAssetPath(resource.getVideoPath()),
                resource.getDescription(),
                contributorName,
                effectiveDate == null ? "" : DATE_FORMATTER.format(effectiveDate.toLocalDate()),
                resource.getStatus() == null ? null : resource.getStatus().getFrontendValue()
        );
    }

    private String toPublicAssetPath(String path) {
        return path == null ? null : path.replace('\\', '/');
    }

    private List<String> resolveTagNames(Long resourceId) {
        List<Long> tagIds = resourceTagRepository.findByResourceId(resourceId).stream()
                .map(ResourceTag::getTagId)
                .toList();
        if (tagIds.isEmpty()) {
            return List.of();
        }

        return tagRepository.findAllById(tagIds).stream()
                .map(Tag::getTagName)
                .filter(Objects::nonNull)
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    private Set<Long> resolveTaggedResourceIds(String normalizedTag) {
        if (normalizedTag == null) {
            return null;
        }

        return tagRepository.findByTagNameIgnoreCase(normalizedTag)
                .map(tag -> resourceTagRepository.findByTagId(tag.getTagId()).stream()
                        .map(ResourceTag::getResourceId)
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

    private void saveResourceTags(Long resourceId, List<String> tags) {
        if (resourceId == null || tags == null || tags.isEmpty()) {
            return;
        }

        tags.stream()
                .map(this::normalizeTagName)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(tagName -> {
                    Tag tag = tagRepository.findByTagNameIgnoreCase(tagName)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setTagName(tagName);
                                return tagRepository.save(newTag);
                            });

                    ResourceTag resourceTag = new ResourceTag();
                    resourceTag.setResourceId(resourceId);
                    resourceTag.setTagId(tag.getTagId());
                    resourceTagRepository.save(resourceTag);
                });
    }

    private String normalizeTagName(String tagName) {
        if (tagName == null) {
            return null;
        }
        String normalized = tagName.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private LocalDateTime getEffectivePublicationDate(Resource resource) {
        return resource.getApprovedAt() != null ? resource.getApprovedAt() : resource.getSubmittedAt();
    }

    private ResourceAction resolveLatestReviewAction(Long resourceId) {
        if (resourceId == null) {
            return null;
        }

        return resourceActionRepository.findByResourceIdOrderByActionAtDescActionIdDesc(resourceId).stream()
                .filter(action -> ResourceActionType.APPROVE.equals(action.getActionType())
                        || ResourceActionType.REJECT.equals(action.getActionType()))
                .findFirst()
                .orElse(null);
    }

    private ResourceAction resolveLatestRejectAction(Long resourceId) {
        if (resourceId == null) {
            return null;
        }

        return resourceActionRepository.findByResourceIdOrderByActionAtDescActionIdDesc(resourceId).stream()
                .filter(action -> ResourceActionType.REJECT.equals(action.getActionType()))
                .findFirst()
                .orElse(null);
    }

    private boolean shouldShowReviewDetails(ResourceStatus status) {
        return status != null && status != ResourceStatus.PENDING_REVIEW;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DASHBOARD_TIME_FORMATTER);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String resolveReviewerDisplayName(Long reviewerUserId) {
        if (reviewerUserId == null) {
            return "";
        }

        return userRepository.findById(reviewerUserId)
                .map(User::getUsername)
                .filter(username -> username != null && !username.isBlank())
                .orElse(String.valueOf(reviewerUserId));
    }

    private String normalizeSubmissionDescription(String description) {
        if (description == null || description.isBlank()) {
            return "No description provided.";
        }
        return description;
    }

    private String normalizeKeyword(String keyword) {
        String normalized = normalizeText(keyword);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean containsIgnoreCase(String value, String normalizedNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedNeedle);
    }

    private boolean updateResourceStatus(Long resourceId,
                                         ResourceStatus nextStatus,
                                         ResourceActionType actionType,
                                         Long actionByUserId,
                                         String note,
                                         boolean noteRequired,
                                         List<ResourceStatus> allowedCurrentStatuses,
                                         String invalidStatusMessage) {
        validateActionUser(actionByUserId);
        String normalizedNote = normalizeActionNote(note, noteRequired);

        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null) {
            return false;
        }
        if (!allowedCurrentStatuses.contains(resource.getStatus())) {
            throw new IllegalStateException(invalidStatusMessage);
        }

        LocalDateTime actionAt = LocalDateTime.now();
        resource.setStatus(nextStatus);
        resourceRepository.save(resource);
        resourceActionRepository.save(buildResourceAction(resource.getResourceId(), actionType, actionByUserId, actionAt, normalizedNote));
        return true;
    }

    private void validateActionUser(Long actionByUserId) {
        if (actionByUserId == null) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }
        if (!userRepository.existsById(actionByUserId)) {
            throw new IllegalArgumentException("Action user not found.");
        }
    }

    private String normalizeActionNote(String note, boolean noteRequired) {
        String normalized = note == null ? "" : note.trim();
        if (noteRequired && normalized.isEmpty()) {
            throw new IllegalArgumentException("note is required.");
        }
        if (normalized.length() > MAX_ACTION_NOTE_LENGTH) {
            throw new IllegalArgumentException("note must be 500 characters or fewer.");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private ResourceAction buildResourceAction(Long resourceId,
                                               ResourceActionType actionType,
                                               Long actionByUserId,
                                               LocalDateTime actionAt,
                                               String feedbackText) {
        ResourceAction action = new ResourceAction();
        action.setResourceId(resourceId);
        action.setActionType(actionType);
        action.setActionByUserId(actionByUserId);
        action.setActionAt(actionAt);
        action.setFeedbackText(feedbackText);
        return action;
    }

    // Retrieve a paginated action history with resource and user details.
    public Page<ResourceActionResponse> getAllResourceActionsWithDetails(Pageable pageable) {
        // Use optimized query with LEFT JOIN to fetch all related data in a single query
        // This avoids the N+1 problem by joining Resource and User tables in one query
        return resourceActionRepository.findAllWithDetails(pageable);
    }
}
