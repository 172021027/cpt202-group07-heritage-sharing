package com.example.heritage_sharing_api.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.heritage_sharing_api.controller.ReviewController;
import com.example.heritage_sharing_api.dto.admin.ReviewDecisionRequestDto;
import com.example.heritage_sharing_api.dto.admin.ReviewDecisionResponseDto;
import com.example.heritage_sharing_api.dto.admin.ReviewDetailDto;
import com.example.heritage_sharing_api.dto.admin.ReviewListItemDto;
import com.example.heritage_sharing_api.dto.admin.ReviewSubmissionDto;
import com.example.heritage_sharing_api.exception.ReviewConflictException;
import com.example.heritage_sharing_api.exception.ReviewNotFoundException;
import com.example.heritage_sharing_api.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Review controller unit tests")
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("UT-RC-01: pending reviews are wrapped using q query parameter")
    void getPendingReviewsReturnsWrappedItemsUsingQ() throws Exception {
        ReviewListItemDto item = new ReviewListItemDto();
        item.setResourceId(301L);
        item.setTitle("Pending title");
        item.setCategory("Dance");
        item.setStatus("pending_review");

        ReviewSubmissionDto submission = new ReviewSubmissionDto();
        submission.setSubmissionId("resource-301");
        submission.setSubmittedAt("2026-04-20T09:30:00");
        item.setSubmissions(List.of(submission));
        // When the mock reviewService in the test receives the call getPendingReviews("heritage"), 
        // it should not execute the real logic, but instead directly return List.of(item).
        when(reviewService.getPendingReviews("heritage")).thenReturn(List.of(item));
        // Simulate an HTTP request using MockMvc, 
        // then check if the HTTP status code and JSON content returned by the Controller match your expectations.
        mockMvc.perform(get("/api/admin/reviews/pending").param("q", "heritage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].resourceId").value(301))
                .andExpect(jsonPath("$.items[0].title").value("Pending title"))
                .andExpect(jsonPath("$.items[0].submissions[0].submissionId").value("resource-301"));
        // Verify that the mock reviewService was actually called, and the call parameter must be "heritage".
        verify(reviewService).getPendingReviews("heritage");
    }

    @Test
    @DisplayName("UT-RC-02: keyword parameter takes precedence over q")
    void getPendingReviewsPrefersKeywordOverQ() throws Exception {
        when(reviewService.getPendingReviews("preferred")).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/reviews/pending")
                        .param("q", "ignored")
                        .param("keyword", "preferred"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        verify(reviewService).getPendingReviews("preferred");
    }

    @Test
    @DisplayName("UT-RC-03: blank keyword falls back to q")
    void getPendingReviewsFallsBackToQWhenKeywordIsBlankAndIgnoresPageSize() throws Exception {
        when(reviewService.getPendingReviews("fallback")).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/reviews/pending")
                        .param("q", "fallback")
                        .param("keyword", "   ")
                        .param("page", "2")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        verify(reviewService).getPendingReviews("fallback");
    }

    @Test
    @DisplayName("UT-RC-04: pending reviews are paged with metadata")
    void getPendingReviewsAppliesPageSizeAndPaginationMetadata() throws Exception {
        when(reviewService.getPendingReviews(null)).thenReturn(List.of(
                reviewListItem(301L),
                reviewListItem(302L),
                reviewListItem(303L),
                reviewListItem(304L),
                reviewListItem(305L)
        ));

        mockMvc.perform(get("/api/admin/reviews/pending")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].resourceId").value(303))
                .andExpect(jsonPath("$.items[1].resourceId").value(304))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));

        verify(reviewService).getPendingReviews(null);
    }

    @Test
    @DisplayName("UT-RC-05: invalid pagination parameters are normalized")
    void getPendingReviewsNormalizesInvalidPaginationParameters() throws Exception {
        when(reviewService.getPendingReviews(null)).thenReturn(List.of(
                reviewListItem(401L),
                reviewListItem(402L)
        ));

        mockMvc.perform(get("/api/admin/reviews/pending")
                        .param("page", "-5")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].resourceId").value(401))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(reviewService).getPendingReviews(null);
    }

    @Test
    @DisplayName("UT-RC-06: review detail is returned when found")
    void getReviewDetailReturnsDtoWhenFoundEvenWithSubmissionId() throws Exception {
        ReviewDetailDto detail = new ReviewDetailDto();
        detail.setResourceId(401L);
        detail.setTitle("Review detail");
        detail.setStatus("pending_review");
        detail.setContributorUsername("alice");

        when(reviewService.getReviewDetail(401L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/reviews/{resourceId}", 401L).param("submissionId", "resource-401"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(401))
                .andExpect(jsonPath("$.title").value("Review detail"))
                .andExpect(jsonPath("$.status").value("pending_review"))
                .andExpect(jsonPath("$.contributorUsername").value("alice"));

        verify(reviewService).getReviewDetail(401L);
    }

    @Test
    @DisplayName("UT-RC-07: missing review detail returns 404")
    void getReviewDetailReturnsNotFoundWhenServiceReturnsNull() throws Exception {
        when(reviewService.getReviewDetail(402L)).thenReturn(null);

        mockMvc.perform(get("/api/admin/reviews/{resourceId}", 402L))
                .andExpect(status().isNotFound());

        verify(reviewService).getReviewDetail(402L);
    }

    @Test
    @DisplayName("UT-RC-08: authenticated Long principal overrides body reviewer id")
    void submitReviewDecisionUsesAuthenticatedLongPrincipalAndOverridesIncomingReviewerId() throws Exception {
        authenticateAs(701L);
        ReviewDecisionRequestDto request = requestBody(501L, "approve", "Looks good", 999L);

        ReviewDecisionResponseDto response = new ReviewDecisionResponseDto();
        response.setSuccess(true);
        response.setResourceId(501L);
        response.setSubmissionId("resource-501");
        response.setStatus("approved");
        response.setActionId(8801L);
        response.setActionAt("2026-04-20T16:00:00");
        response.setMessage("Review decision recorded successfully.");

        when(reviewService.submitReviewDecision(any(ReviewDecisionRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resourceId").value(501))
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.actionId").value(8801));

        ArgumentCaptor<ReviewDecisionRequestDto> captor = ArgumentCaptor.forClass(ReviewDecisionRequestDto.class);
        verify(reviewService).submitReviewDecision(captor.capture());
        assertEquals(701L, captor.getValue().getActionByUserId());
        assertEquals("approve", captor.getValue().getDecision());
        assertEquals("Looks good", captor.getValue().getNote());
    }

    @Test
    @DisplayName("UT-RC-09: numeric String principal is accepted")
    void submitReviewDecisionAcceptsNumericStringPrincipal() throws Exception {
        authenticateAs("702");
        ReviewDecisionRequestDto request = requestBody(502L, "reject", "Needs revision", null);

        ReviewDecisionResponseDto response = new ReviewDecisionResponseDto();
        response.setSuccess(true);
        response.setResourceId(502L);
        response.setSubmissionId("resource-502");
        response.setStatus("rejected");
        response.setActionId(8802L);
        response.setActionAt("2026-04-20T16:10:00");
        response.setMessage("Review decision recorded successfully.");

        when(reviewService.submitReviewDecision(any(ReviewDecisionRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"));

        ArgumentCaptor<ReviewDecisionRequestDto> captor = ArgumentCaptor.forClass(ReviewDecisionRequestDto.class);
        verify(reviewService).submitReviewDecision(captor.capture());
        assertEquals(702L, captor.getValue().getActionByUserId());
    }

    @Test
    @DisplayName("UT-RC-10: missing authentication returns bad request")
    void submitReviewDecisionReturnsBadRequestWhenAuthenticationIsMissing() throws Exception {
        ReviewDecisionRequestDto request = requestBody(503L, "approve", "Looks good", null);

        mockMvc.perform(post("/api/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authenticated user is required."));

        verify(reviewService, never()).submitReviewDecision(any(ReviewDecisionRequestDto.class));
    }

    @Test
    @DisplayName("UT-RC-11: non-numeric principal returns bad request")
    void submitReviewDecisionReturnsBadRequestWhenPrincipalCannotBeParsed() throws Exception {
        authenticateAs("abc");
        ReviewDecisionRequestDto request = requestBody(504L, "approve", "Looks good", null);

        mockMvc.perform(post("/api/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authenticated user is required."));

        verify(reviewService, never()).submitReviewDecision(any(ReviewDecisionRequestDto.class));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("UT-RC-12: service exceptions map to stable HTTP responses")
    @MethodSource("submitDecisionFailures")
    void submitReviewDecisionMapsServiceExceptions(String scenario,
                                                   RuntimeException exception,
                                                   int expectedStatus,
                                                   String expectedMessage) throws Exception {
        authenticateAs(703L);
        ReviewDecisionRequestDto request = requestBody(505L, "approve", "Looks good", null);
        when(reviewService.submitReviewDecision(any(ReviewDecisionRequestDto.class))).thenThrow(exception);

        mockMvc.perform(post("/api/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(expectedMessage));

        verify(reviewService).submitReviewDecision(any(ReviewDecisionRequestDto.class));
    }

    private static Stream<Arguments> submitDecisionFailures() {
        return Stream.of(
                Arguments.of("not found", new ReviewNotFoundException("Resource not found."), 404, "Resource not found."),
                Arguments.of("conflict", new ReviewConflictException("Resource is no longer pending review."), 409,
                        "Resource is no longer pending review."),
                Arguments.of("bad request", new IllegalArgumentException("decision is required."), 400,
                        "decision is required."),
                Arguments.of("unexpected error", new RuntimeException("boom"), 500,
                        "Failed to submit review decision.")
        );
    }

    private void authenticateAs(Object principal) {
        // Standalone MockMvc does not run the JWT filter, so tests set the principal directly.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static ReviewListItemDto reviewListItem(Long resourceId) {
        ReviewListItemDto item = new ReviewListItemDto();
        item.setResourceId(resourceId);
        item.setTitle("Title-" + resourceId);
        item.setCategory("Category");
        item.setStatus("pending_review");
        item.setSubmissions(List.of());
        return item;
    }

    private static ReviewDecisionRequestDto requestBody(Long resourceId,
                                                        String decision,
                                                        String note,
                                                        Long actionByUserId) {
        ReviewDecisionRequestDto request = new ReviewDecisionRequestDto();
        request.setResourceId(resourceId);
        request.setSubmissionId("resource-" + resourceId);
        request.setDecision(decision);
        request.setNote(note);
        request.setActionByUserId(actionByUserId);
        return request;
    }
}
