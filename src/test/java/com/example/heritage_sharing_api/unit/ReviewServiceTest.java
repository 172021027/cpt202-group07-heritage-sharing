package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.dto.admin.ReviewDecisionRequestDto;
import com.example.heritage_sharing_api.dto.admin.ReviewDecisionResponseDto;
import com.example.heritage_sharing_api.dto.admin.ReviewDetailDto;
import com.example.heritage_sharing_api.dto.admin.ReviewListItemDto;
import com.example.heritage_sharing_api.entity.Category;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceAction;
import com.example.heritage_sharing_api.entity.ResourceActionType;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.Tag;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.exception.ReviewConflictException;
import com.example.heritage_sharing_api.exception.ReviewNotFoundException;
import com.example.heritage_sharing_api.repository.CategoryRepository;
import com.example.heritage_sharing_api.repository.ResourceActionRepository;
import com.example.heritage_sharing_api.repository.ResourceRepository;
import com.example.heritage_sharing_api.repository.ResourceTagRepository;
import com.example.heritage_sharing_api.repository.TagRepository;
import com.example.heritage_sharing_api.repository.UserRepository;
import com.example.heritage_sharing_api.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Review service unit tests")
class ReviewServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceActionRepository resourceActionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResourceTagRepository resourceTagRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("UT-RS-01: pending reviews load without search query")
    void getPendingReviewsWithoutQueryLoadsPendingResourcesAndMapsListItems() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 4, 20, 9, 30, 0);
        Resource resource = createResource(101L, 11L, 7L, ResourceStatus.PENDING_REVIEW, submittedAt, null);
        resource.setDescription("Pending description");
        resource.setPicturePath("/uploads/pending.png");
        resource.setVideoPath("/uploads/pending.mp4");

        when(resourceRepository.findByStatusInOrderBySubmittedAtDesc(List.of(ResourceStatus.PENDING_REVIEW)))
                .thenReturn(List.of(resource));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(new Category(7L, "Architecture")));

        List<ReviewListItemDto> items = reviewService.getPendingReviews(null);

        assertEquals(1, items.size());
        ReviewListItemDto item = items.getFirst();
        assertEquals(101L, item.getResourceId());
        assertEquals("Title-101", item.getTitle());
        assertEquals("Architecture", item.getCategory());
        assertEquals("pending_review", item.getStatus());
        assertEquals(1, item.getSubmissions().size());
        assertEquals("resource-101", item.getSubmissions().getFirst().getSubmissionId());
        assertEquals("2026-04-20T09:30:00", item.getSubmissions().getFirst().getSubmittedAt());
        assertEquals("Pending description", item.getSubmissions().getFirst().getSummary());
        assertEquals("Pending description", item.getSubmissions().getFirst().getContent());
        assertEquals("/uploads/pending.png", item.getSubmissions().getFirst().getPicture());
        assertEquals("/uploads/pending.mp4", item.getSubmissions().getFirst().getVideo());

        verify(resourceRepository).findByStatusInOrderBySubmittedAtDesc(List.of(ResourceStatus.PENDING_REVIEW));
    }

    @Test
    @DisplayName("UT-RS-02: pending review search trims query text")
    void getPendingReviewsWithQueryTrimsAndUsesSearchRepository() {
        Resource resource = createResource(102L, 12L, 99L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 10, 0, 0), null);

        when(resourceRepository.findByStatusInAndTitleContainingIgnoreCaseOrderBySubmittedAtDesc(
                List.of(ResourceStatus.PENDING_REVIEW), "keyword"))
                .thenReturn(List.of(resource));
        when(categoryRepository.findAllById(any())).thenReturn(List.of());

        List<ReviewListItemDto> items = reviewService.getPendingReviews("  keyword  ");

        assertEquals(1, items.size());
        assertEquals("99", items.getFirst().getCategory());
        verify(resourceRepository).findByStatusInAndTitleContainingIgnoreCaseOrderBySubmittedAtDesc(
                List.of(ResourceStatus.PENDING_REVIEW), "keyword");
    }

    @Test
    @DisplayName("UT-RS-03: pending review list handles missing categories")
    void getPendingReviewsFallsBackForMissingCategories() {
        Resource withoutCategory = createResource(103L, 13L, null, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 11, 0, 0), null);
        Resource unknownCategory = createResource(104L, 14L, 404L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 11, 5, 0), null);

        when(resourceRepository.findByStatusInOrderBySubmittedAtDesc(List.of(ResourceStatus.PENDING_REVIEW)))
                .thenReturn(List.of(withoutCategory, unknownCategory));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(new Category(1L, "Unused")));

        List<ReviewListItemDto> items = reviewService.getPendingReviews("");

        assertEquals("unknown", items.get(0).getCategory());
        assertEquals("404", items.get(1).getCategory());
    }

    @Test
    @DisplayName("UT-RS-04: empty pending list skips lookup work")
    void getPendingReviewsSkipsCategoryLookupWhenNoPendingResourcesExist() {
        when(resourceRepository.findByStatusInOrderBySubmittedAtDesc(List.of(ResourceStatus.PENDING_REVIEW)))
                .thenReturn(List.of());

        List<ReviewListItemDto> items = reviewService.getPendingReviews(null);

        assertTrue(items.isEmpty());
        verifyNoInteractions(categoryRepository, userRepository, resourceTagRepository, tagRepository);
    }

    @Test
    @DisplayName("UT-RS-05: missing resource returns null detail")
    void getReviewDetailReturnsNullWhenResourceMissing() {
        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

        ReviewDetailDto detail = reviewService.getReviewDetail(999L);

        assertNull(detail);
        verifyNoInteractions(categoryRepository, userRepository, resourceTagRepository, tagRepository);
    }

    @Test
    @DisplayName("UT-RS-06: review detail maps fields and tags")
    void getReviewDetailMapsFieldsAndSortedDistinctTags() {
        Resource resource = createResource(105L, 77L, 3L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 12, 15, 0), LocalDateTime.of(2026, 4, 20, 13, 0, 0));
        resource.setDescription("Resource detail body");
        resource.setLocation("Shenzhen");
        resource.setPicturePath("/uploads/detail.png");
        resource.setVideoPath("/uploads/detail.mp4");
        resource.setCopyrightDeclaration("Copyright statement");

        when(resourceRepository.findById(105L)).thenReturn(Optional.of(resource));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(new Category(3L, "Dance")));
        when(userRepository.findById(77L)).thenReturn(Optional.of(new User(77L, "alice", "alice@example.com",
                "hash", null, null, "user")));
        when(resourceTagRepository.findTagsByResourceId(105L)).thenReturn(List.of(
                new Tag(2L, "alpha"),
                new Tag(5L, "Beta"),
                new Tag(9L, "Zulu")
        ));

        ReviewDetailDto detail = reviewService.getReviewDetail(105L);

        assertNotNull(detail);
        assertEquals(105L, detail.getResourceId());
        assertEquals(77L, detail.getContributorId());
        assertEquals("alice", detail.getContributorUsername());
        assertEquals("Title-105", detail.getTitle());
        assertEquals("Resource detail body", detail.getDescription());
        assertEquals(3L, detail.getCategoryId());
        assertEquals("Dance", detail.getCategory());
        assertEquals("Shenzhen", detail.getLocation());
        assertEquals("/uploads/detail.png", detail.getPicturePath());
        assertEquals("/uploads/detail.mp4", detail.getVideoPath());
        assertEquals("Copyright statement", detail.getCopyrightDeclaration());
        assertEquals("pending_review", detail.getStatus());
        assertEquals("2026-04-20T12:15:00", detail.getSubmittedAt());
        assertEquals("2026-04-20T13:00:00", detail.getApprovedAt());
        assertEquals(List.of("alpha", "Beta", "Zulu"),
                detail.getTags().stream().map(ReviewDetailDto.ReviewTagDto::getTagName).toList());
    }

    @Test
    @DisplayName("UT-RS-07: missing contributor falls back to contributor id")
    void getReviewDetailUsesContributorIdWhenUserMissing() {
        Resource resource = createResource(106L, 88L, 5L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 12, 30, 0), null);

        when(resourceRepository.findById(106L)).thenReturn(Optional.of(resource));
        when(userRepository.findById(88L)).thenReturn(Optional.empty());

        ReviewDetailDto detail = reviewService.getReviewDetail(106L);

        assertEquals("88", detail.getContributorUsername());
    }

    @Test
    @DisplayName("UT-RS-08: blank contributor username falls back to contributor id")
    void getReviewDetailUsesContributorIdWhenUsernameIsBlank() {
        Resource resource = createResource(107L, 89L, 5L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 12, 35, 0), null);

        when(resourceRepository.findById(107L)).thenReturn(Optional.of(resource));
        when(userRepository.findById(89L)).thenReturn(Optional.of(new User(89L, "   ", "blank@example.com",
                "hash", null, null, "user")));

        ReviewDetailDto detail = reviewService.getReviewDetail(107L);

        assertEquals("89", detail.getContributorUsername());
    }

    @Test
    @DisplayName("UT-RS-09: missing contributor id is rendered as unknown")
    void getReviewDetailUsesUnknownWhenContributorIdIsMissing() {
        Resource resource = createResource(108L, null, 5L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 12, 40, 0), null);

        when(resourceRepository.findById(108L)).thenReturn(Optional.of(resource));

        ReviewDetailDto detail = reviewService.getReviewDetail(108L);

        assertEquals("unknown", detail.getContributorUsername());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("UT-RS-10: detail returns empty tags when none are assigned")
    void getReviewDetailReturnsEmptyTagsWhenNoTagsAssigned() {
        Resource resource = createResource(109L, 90L, 5L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 12, 45, 0), null);

        when(resourceRepository.findById(109L)).thenReturn(Optional.of(resource));
        when(userRepository.findById(90L)).thenReturn(Optional.of(new User(90L, "bob", "bob@example.com",
                "hash", null, null, "user")));

        ReviewDetailDto detail = reviewService.getReviewDetail(109L);

        assertTrue(detail.getTags().isEmpty());
        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("UT-RS-11: null detail fields are rendered safely")
    void getReviewDetailHandlesNullDatesAndCategoryIdWithoutFallbackQueries() {
        Resource resource = createResource(110L, null, null, ResourceStatus.PENDING_REVIEW, null, null);

        when(resourceRepository.findById(110L)).thenReturn(Optional.of(resource));

        ReviewDetailDto detail = reviewService.getReviewDetail(110L);

        assertNotNull(detail);
        assertEquals("unknown", detail.getCategory());
        assertEquals("unknown", detail.getContributorUsername());
        assertEquals("", detail.getSubmittedAt());
        assertEquals("", detail.getApprovedAt());
        assertTrue(detail.getTags().isEmpty());
        verify(categoryRepository, never()).findById(anyLong());
        verify(userRepository, never()).findById(anyLong());
        verify(resourceTagRepository).findTagsByResourceId(110L);
        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("UT-RS-12: approving a pending resource records an approve action")
    void submitReviewDecisionApprovesPendingResourceAndRecordsAction() {
        ReviewDecisionRequestDto request = validRequest(201L, 501L, "  Approve  ", "  Looks good  ");
        Resource pending = createResource(201L, 41L, 6L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 14, 0, 0), null);

        when(userRepository.existsById(501L)).thenReturn(true);
        when(resourceRepository.findById(201L)).thenReturn(Optional.of(pending));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceActionRepository.save(any(ResourceAction.class))).thenAnswer(invocation -> {
            ResourceAction action = invocation.getArgument(0);
            action.setActionId(9001L);
            return action;
        });

        ReviewDecisionResponseDto response = reviewService.submitReviewDecision(request);

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        ArgumentCaptor<ResourceAction> actionCaptor = ArgumentCaptor.forClass(ResourceAction.class);
        verify(resourceRepository).save(resourceCaptor.capture());
        verify(resourceActionRepository).save(actionCaptor.capture());

        Resource savedResource = resourceCaptor.getValue();
        ResourceAction savedAction = actionCaptor.getValue();

        assertEquals(ResourceStatus.APPROVED, savedResource.getStatus());
        assertNotNull(savedResource.getApprovedAt());
        assertEquals(201L, savedAction.getResourceId());
        assertEquals(ResourceActionType.APPROVE, savedAction.getActionType());
        assertEquals(501L, savedAction.getActionByUserId());
        assertEquals("Looks good", savedAction.getFeedbackText());
        assertNotNull(savedAction.getActionAt());

        assertTrue(response.isSuccess());
        assertEquals(201L, response.getResourceId());
        assertEquals("resource-201", response.getSubmissionId());
        assertEquals("approved", response.getStatus());
        assertEquals(9001L, response.getActionId());
        assertEquals(savedAction.getActionAt(), LocalDateTime.parse(response.getActionAt()));
        assertEquals("Review decision recorded successfully.", response.getMessage());
    }

    @Test
    @DisplayName("UT-RS-13: rejecting a pending resource records a reject action")
    void submitReviewDecisionRejectsPendingResourceAndClearsApprovedAt() {
        ReviewDecisionRequestDto request = validRequest(202L, 502L, "reject", "Needs revision");
        Resource pending = createResource(202L, 42L, 7L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 14, 30, 0), LocalDateTime.of(2026, 4, 20, 14, 45, 0));

        when(userRepository.existsById(502L)).thenReturn(true);
        when(resourceRepository.findById(202L)).thenReturn(Optional.of(pending));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceActionRepository.save(any(ResourceAction.class))).thenAnswer(invocation -> {
            ResourceAction action = invocation.getArgument(0);
            action.setActionId(9002L);
            return action;
        });

        ReviewDecisionResponseDto response = reviewService.submitReviewDecision(request);

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        ArgumentCaptor<ResourceAction> actionCaptor = ArgumentCaptor.forClass(ResourceAction.class);
        verify(resourceRepository).save(resourceCaptor.capture());
        verify(resourceActionRepository).save(actionCaptor.capture());

        assertEquals(ResourceStatus.REJECTED, resourceCaptor.getValue().getStatus());
        assertNull(resourceCaptor.getValue().getApprovedAt());
        assertEquals(ResourceActionType.REJECT, actionCaptor.getValue().getActionType());
        assertEquals("Needs revision", actionCaptor.getValue().getFeedbackText());
        assertEquals("rejected", response.getStatus());
        assertEquals(9002L, response.getActionId());
    }

    @Test
    @DisplayName("UT-RS-14: review note accepts the 500 character boundary")
    void submitReviewDecisionAllowsExactlyFiveHundredCharactersInNote() {
        String note = "a".repeat(500);
        ReviewDecisionRequestDto request = validRequest(205L, 505L, "approve", note);
        Resource pending = createResource(205L, 45L, 8L, ResourceStatus.PENDING_REVIEW,
                LocalDateTime.of(2026, 4, 20, 15, 20, 0), null);

        when(userRepository.existsById(505L)).thenReturn(true);
        when(resourceRepository.findById(205L)).thenReturn(Optional.of(pending));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceActionRepository.save(any(ResourceAction.class))).thenAnswer(invocation -> {
            ResourceAction action = invocation.getArgument(0);
            action.setActionId(9005L);
            return action;
        });

        ReviewDecisionResponseDto response = reviewService.submitReviewDecision(request);

        ArgumentCaptor<ResourceAction> actionCaptor = ArgumentCaptor.forClass(ResourceAction.class);
        verify(resourceActionRepository).save(actionCaptor.capture());
        assertEquals(500, actionCaptor.getValue().getFeedbackText().length());
        assertEquals("approved", response.getStatus());
    }

    @Test
    @DisplayName("UT-RS-15: missing resource throws ReviewNotFoundException")
    void submitReviewDecisionThrowsWhenResourceIsMissing() {
        ReviewDecisionRequestDto request = validRequest(203L, 503L, "approve", "Looks good");

        when(userRepository.existsById(503L)).thenReturn(true);
        when(resourceRepository.findById(203L)).thenReturn(Optional.empty());

        ReviewNotFoundException exception = assertThrows(ReviewNotFoundException.class,
                () -> reviewService.submitReviewDecision(request));

        assertEquals("Resource not found.", exception.getMessage());
        verify(resourceRepository, never()).save(any(Resource.class));
        verifyNoInteractions(resourceActionRepository);
    }

    @Test
    @DisplayName("UT-RS-16: non-pending resource throws ReviewConflictException")
    void submitReviewDecisionThrowsConflictWhenResourceIsNotPending() {
        ReviewDecisionRequestDto request = validRequest(204L, 504L, "approve", "Looks good");
        Resource approved = createResource(204L, 44L, 8L, ResourceStatus.APPROVED,
                LocalDateTime.of(2026, 4, 20, 15, 0, 0), LocalDateTime.of(2026, 4, 20, 15, 15, 0));

        when(userRepository.existsById(504L)).thenReturn(true);
        when(resourceRepository.findById(204L)).thenReturn(Optional.of(approved));

        ReviewConflictException exception = assertThrows(ReviewConflictException.class,
                () -> reviewService.submitReviewDecision(request));

        assertEquals("Resource is no longer pending review.", exception.getMessage());
        verify(resourceRepository, never()).save(any(Resource.class));
        verifyNoInteractions(resourceActionRepository);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("UT-RS-17: every canonical non-pending status is rejected")
    @MethodSource("nonPendingStatuses")
    void submitReviewDecisionRejectsEveryCanonicalNonPendingStatus(ResourceStatus status) {
        ReviewDecisionRequestDto request = validRequest(240L, 540L, "approve", "Looks good");
        Resource resource = createResource(240L, 44L, 8L, status,
                LocalDateTime.of(2026, 4, 20, 15, 30, 0), null);

        when(userRepository.existsById(540L)).thenReturn(true);
        when(resourceRepository.findById(240L)).thenReturn(Optional.of(resource));

        ReviewConflictException exception = assertThrows(ReviewConflictException.class,
                () -> reviewService.submitReviewDecision(request));

        assertEquals("Resource is no longer pending review.", exception.getMessage());
        verify(resourceRepository, never()).save(any(Resource.class));
        verifyNoInteractions(resourceActionRepository);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("UT-RS-18: review decision validates request fields")
    @MethodSource("invalidDecisionRequests")
    void submitReviewDecisionValidatesRequestFields(String scenario,
                                                    ReviewDecisionRequestDto request,
                                                    boolean reviewerExists,
                                                    String expectedMessage) {
        if (request != null && request.getActionByUserId() != null && !reviewerExists) {
            when(userRepository.existsById(request.getActionByUserId())).thenReturn(false);
        }

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReviewDecision(request));

        assertEquals(expectedMessage, exception.getMessage(), scenario);
        verify(resourceRepository, never()).findById(anyLong());
        verify(resourceRepository, never()).save(any(Resource.class));
        verifyNoInteractions(resourceActionRepository);
    }

    private static Stream<Arguments> invalidDecisionRequests() {
        ReviewDecisionRequestDto missingResourceId = validRequest(301L, 601L, "approve", "note");
        missingResourceId.setResourceId(null);

        ReviewDecisionRequestDto blankDecision = validRequest(302L, 602L, "approve", "note");
        blankDecision.setDecision("   ");

        ReviewDecisionRequestDto invalidDecision = validRequest(303L, 603L, "approve", "note");
        invalidDecision.setDecision("hold");

        ReviewDecisionRequestDto blankNote = validRequest(304L, 604L, "approve", "note");
        blankNote.setNote("   ");

        ReviewDecisionRequestDto longNote = validRequest(305L, 605L, "approve", "note");
        longNote.setNote("a".repeat(501));

        ReviewDecisionRequestDto missingReviewer = validRequest(306L, 606L, "approve", "note");
        missingReviewer.setActionByUserId(null);

        ReviewDecisionRequestDto unknownReviewer = validRequest(307L, 607L, "approve", "note");

        return Stream.of(
                Arguments.of("null request", null, true, "Request body is required."),
                Arguments.of("missing resourceId", missingResourceId, true, "resourceId is required."),
                Arguments.of("blank decision", blankDecision, true, "decision is required."),
                Arguments.of("invalid decision", invalidDecision, true, "decision must be approve or reject."),
                Arguments.of("blank note", blankNote, true, "note is required."),
                Arguments.of("note too long", longNote, true, "note must be 500 characters or fewer."),
                Arguments.of("missing reviewer", missingReviewer, true, "actionByUserId is required."),
                Arguments.of("unknown reviewer", unknownReviewer, false, "Reviewer account not found.")
        );
    }

    private static Stream<ResourceStatus> nonPendingStatuses() {
        // The final Review contract accepts only the canonical PENDING_REVIEW enum for decisions.
        Set<ResourceStatus> statuses = EnumSet.allOf(ResourceStatus.class);
        statuses.remove(ResourceStatus.PENDING_REVIEW);
        return statuses.stream();
    }

    private static ReviewDecisionRequestDto validRequest(Long resourceId, Long reviewerId, String decision, String note) {
        ReviewDecisionRequestDto request = new ReviewDecisionRequestDto();
        request.setResourceId(resourceId);
        request.setSubmissionId("resource-" + resourceId);
        request.setDecision(decision);
        request.setNote(note);
        request.setActionByUserId(reviewerId);
        return request;
    }

    private static Resource createResource(Long resourceId,
                                           Long contributorId,
                                           Long categoryId,
                                           ResourceStatus status,
                                           LocalDateTime submittedAt,
                                           LocalDateTime approvedAt) {
        Resource resource = new Resource();
        resource.setResourceId(resourceId);
        resource.setContributorId(contributorId);
        resource.setTitle("Title-" + resourceId);
        resource.setDescription("Description-" + resourceId);
        resource.setCategoryId(categoryId);
        resource.setLocation("Location-" + resourceId);
        resource.setPicturePath("/uploads/" + resourceId + ".png");
        resource.setVideoPath("/uploads/" + resourceId + ".mp4");
        resource.setCopyrightDeclaration("Copyright-" + resourceId);
        resource.setStatus(status);
        resource.setSubmittedAt(submittedAt);
        resource.setApprovedAt(approvedAt);
        return resource;
    }
}
