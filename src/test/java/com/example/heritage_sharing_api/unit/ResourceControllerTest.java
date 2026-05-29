package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.controller.ResourceController;
import com.example.heritage_sharing_api.dto.PublicResourceDto;
import com.example.heritage_sharing_api.dto.SubmitResourceRequest;
import com.example.heritage_sharing_api.dto.admin.ResourceActionRequestDto;
import com.example.heritage_sharing_api.entity.ContributorRequestStatus;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.service.ResourceService;
import com.example.heritage_sharing_api.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for ResourceController - Search, Filter and Offline functionality")
class ResourceControllerTest {

    @Mock
    private ResourceService resourceService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ResourceController resourceController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Test Data for Search and Filter
    private PublicResourceDto testResource1;
    private PublicResourceDto testResource2;

    // Constants for Offline Resource
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_RESOURCE_ID = 100L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(resourceController).build();
        objectMapper = new ObjectMapper();

        // Setup Test Resources
        testResource1 = new PublicResourceDto(
                1L, 1L, 1L,
                "Ancient Temple", "A beautiful ancient temple", "Architecture",
                Arrays.asList("ancient", "temple"), "Beijing",
                "uploads/image/temple.jpg", "uploads/video/temple.mp4",
                "A beautiful ancient temple", "Contributor1",
                "2024-01-15", "approved"
        );

        testResource2 = new PublicResourceDto(
                2L, 2L, 2L,
                "Traditional Painting", "A classic Chinese painting", "Art",
                Arrays.asList("painting", "traditional"), "Shanghai",
                "uploads/image/painting.jpg", "uploads/video/painting.mp4",
                "A classic Chinese painting", "Contributor2",
                "2024-02-20", "approved"
        );

        // Setup security context with authenticated user for offline tests
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USER_ID, null));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================================================================================
    // Search and Filter Functionality Tests
    // ===================================================================================

    @Test
    @DisplayName("UT-RC-01: searchResources returns resources matching keyword in title")
    void searchResourcesReturnsResourcesMatchingKeywordInTitle() throws Exception {
        when(resourceService.searchResources("temple")).thenReturn(List.of(testResource1));

        mockMvc.perform(get("/api/resources/search")
                        .param("keyword", "temple")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(testResource1))));
    }

    @Test
    @DisplayName("UT-RC-02: searchResources returns empty list when no matches")
    void searchResourcesReturnsEmptyListWhenNoMatches() throws Exception {
        when(resourceService.searchResources("nonexistent")).thenReturn(List.of());

        mockMvc.perform(get("/api/resources/search")
                        .param("keyword", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("UT-RC-03: searchResources returns all approved resources when keyword is null")
    void searchResourcesReturnsAllApprovedResourcesWhenKeywordIsNull() throws Exception {
        when(resourceService.searchResources(null)).thenReturn(Arrays.asList(testResource1, testResource2));

        mockMvc.perform(get("/api/resources/search")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(Arrays.asList(testResource1, testResource2))));
    }

    @Test
    @DisplayName("UT-RC-04: searchResources returns all approved resources when keyword is empty")
    void searchResourcesReturnsAllApprovedResourcesWhenKeywordIsEmpty() throws Exception {
        when(resourceService.searchResources("")).thenReturn(Arrays.asList(testResource1, testResource2));

        mockMvc.perform(get("/api/resources/search")
                        .param("keyword", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(Arrays.asList(testResource1, testResource2))));
    }

    @Test
    @DisplayName("UT-RC-05: filterResources returns resources matching category")
    void filterResourcesReturnsResourcesMatchingCategory() throws Exception {
        when(resourceService.filterResources(1L, null, null)).thenReturn(List.of(testResource1));

        mockMvc.perform(get("/api/resources/filter")
                        .param("categoryId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(testResource1))));
    }

    @Test
    @DisplayName("UT-RC-06: filterResources returns resources matching tag")
    void filterResourcesReturnsResourcesMatchingTag() throws Exception {
        when(resourceService.filterResources(null, "ancient", null)).thenReturn(List.of(testResource1));

        mockMvc.perform(get("/api/resources/filter")
                        .param("tag", "ancient")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(testResource1))));
    }

    @Test
    @DisplayName("UT-RC-07: filterResources returns resources matching location")
    void filterResourcesReturnsResourcesMatchingLocation() throws Exception {
        when(resourceService.filterResources(null, null, "Beijing")).thenReturn(List.of(testResource1));

        mockMvc.perform(get("/api/resources/filter")
                        .param("location", "Beijing")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(testResource1))));
    }

    @Test
    @DisplayName("UT-RC-08: filterResources returns resources matching multiple filters")
    void filterResourcesReturnsResourcesMatchingMultipleFilters() throws Exception {
        when(resourceService.filterResources(1L, "ancient", "Beijing")).thenReturn(List.of(testResource1));

        mockMvc.perform(get("/api/resources/filter")
                        .param("categoryId", "1")
                        .param("tag", "ancient")
                        .param("location", "Beijing")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(testResource1))));
    }

    @Test
    @DisplayName("UT-RC-09: filterResources returns empty list when no matches")
    void filterResourcesReturnsEmptyListWhenNoMatches() throws Exception {
        when(resourceService.filterResources(99L, "nonexistent", "Nowhere")).thenReturn(List.of());

        mockMvc.perform(get("/api/resources/filter")
                        .param("categoryId", "99")
                        .param("tag", "nonexistent")
                        .param("location", "Nowhere")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("UT-RC-10: filterResources returns all approved resources when no filters provided")
    void filterResourcesReturnsAllApprovedResourcesWhenNoFiltersProvided() throws Exception {
        when(resourceService.filterResources(null, null, null)).thenReturn(Arrays.asList(testResource1, testResource2));

        mockMvc.perform(get("/api/resources/filter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(Arrays.asList(testResource1, testResource2))));
    }

    @Test
    @DisplayName("UT-RC-11: searchAndFilterResources returns resources matching keyword and category")
    void searchAndFilterResourcesReturnsResourcesMatchingKeywordAndCategory() throws Exception {
        when(resourceService.searchAndFilterResources("temple", 1L, null, null)).thenReturn(List.of(testResource1));

        mockMvc.perform(get("/api/resources/search-and-filter")
                        .param("keyword", "temple")
                        .param("categoryId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(testResource1))));
    }

    @Test
    @DisplayName("UT-RC-12: searchAndFilterResources returns resources matching all criteria")
    void searchAndFilterResourcesReturnsResourcesMatchingAllCriteria() throws Exception {
        when(resourceService.searchAndFilterResources("temple", 1L, "ancient", "Beijing"))
                .thenReturn(List.of(testResource1));

        mockMvc.perform(get("/api/resources/search-and-filter")
                        .param("keyword", "temple")
                        .param("categoryId", "1")
                        .param("tag", "ancient")
                        .param("location", "Beijing")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(testResource1))));
    }

    @Test
    @DisplayName("UT-RC-13: searchAndFilterResources returns empty list when no matches found")
    void searchAndFilterResourcesReturnsEmptyListWhenNoMatchesFound() throws Exception {
        when(resourceService.searchAndFilterResources("painting", 1L, "ancient", "Beijing"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/resources/search-and-filter")
                        .param("keyword", "painting")
                        .param("categoryId", "1")
                        .param("tag", "ancient")
                        .param("location", "Beijing")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("UT-RC-14: submitResource returns success when contributor submits valid data")
    void submitResourceReturnsSuccessWhenContributorSubmitsValidData() throws Exception {
        User contributor = contributorUser();
        Resource savedResource = savedResource();
        allowContributorSubmission(contributor);
        when(resourceService.submitResource(any(SubmitResourceRequest.class), isNull(), isNull()))
                .thenReturn(savedResource);

        ResponseEntity<Map<String, Object>> response = resourceController.submitResource(
                "Shadow Puppetry",
                "Xi'an",
                3L,
                "Traditional performance archive",
                List.of("performance", "intangible"),
                "I own the rights",
                null,
                null
        );

        ArgumentCaptor<SubmitResourceRequest> requestCaptor = ArgumentCaptor.forClass(SubmitResourceRequest.class);
        verify(resourceService).submitResource(requestCaptor.capture(), isNull(), isNull());

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(true, response.getBody().get("success"));
        Assertions.assertEquals("Resource submitted successfully.", response.getBody().get("message"));
        Assertions.assertEquals(10L, response.getBody().get("resourceId"));
        Assertions.assertEquals(TEST_USER_ID, requestCaptor.getValue().getContributorId());
    }

    @Test
    @DisplayName("UT-RC-15: submitResource returns unauthorized when user is not authenticated")
    void submitResourceReturnsUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        ResponseEntity<Map<String, Object>> response = resourceController.submitResource(
                "Shadow Puppetry",
                "Xi'an",
                3L,
                "Traditional performance archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(false, response.getBody().get("success"));
        Assertions.assertEquals("Authenticated user is required.", response.getBody().get("message"));
        verify(resourceService, never()).submitResource(any(SubmitResourceRequest.class), any(), any());
    }

    @Test
    @DisplayName("UT-RC-16: submitResource returns forbidden when user is not contributor")
    void submitResourceReturnsForbiddenWhenUserIsNotContributor() throws Exception {
        User regularUser = regularUser();
        when(userService.getUserById(TEST_USER_ID)).thenReturn(Optional.of(regularUser));
        when(userService.isContributor(regularUser)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = resourceController.submitResource(
                "Shadow Puppetry",
                "Xi'an",
                3L,
                "Traditional performance archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(false, response.getBody().get("success"));
        Assertions.assertEquals("Only Contributor users can submit resources.", response.getBody().get("message"));
        verify(resourceService, never()).submitResource(any(SubmitResourceRequest.class), any(), any());
    }

    @Test
    @DisplayName("UT-RC-17: submitResource returns bad request when upload fails")
    void submitResourceReturnsBadRequestWhenUploadFails() throws Exception {
        allowContributorSubmission(contributorUser());
        when(resourceService.submitResource(any(SubmitResourceRequest.class), isNull(), isNull()))
                .thenThrow(new IOException("disk full"));

        ResponseEntity<Map<String, Object>> response = resourceController.submitResource(
                "Shadow Puppetry",
                "Xi'an",
                3L,
                "Traditional performance archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(false, response.getBody().get("success"));
        Assertions.assertEquals("Error uploading files: disk full", response.getBody().get("message"));
    }

    @Test
    @DisplayName("UT-RC-18: submitResource returns bad request when title is empty")
    void submitResourceReturnsBadRequestWhenTitleIsEmpty() throws Exception {
        allowContributorSubmission(contributorUser());
        when(resourceService.submitResource(any(SubmitResourceRequest.class), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("Title is required."));

        ResponseEntity<Map<String, Object>> response = resourceController.submitResource(
                "",
                "Xi'an",
                3L,
                "Traditional performance archive",
                List.of("performance"),
                "I own the rights",
                null,
                null
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(false, response.getBody().get("success"));
        Assertions.assertEquals("Error submitting resource: Title is required.", response.getBody().get("message"));
    }

    @Test
    @DisplayName("UT-RC-19: submitResource returns bad request when tags are empty")
    void submitResourceReturnsBadRequestWhenTagsAreEmpty() throws Exception {
        allowContributorSubmission(contributorUser());
        when(resourceService.submitResource(any(SubmitResourceRequest.class), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("At least one tag is required."));

        ResponseEntity<Map<String, Object>> response = resourceController.submitResource(
                "Shadow Puppetry",
                "Xi'an",
                3L,
                "Traditional performance archive",
                List.of(),
                "I own the rights",
                null,
                null
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(false, response.getBody().get("success"));
        Assertions.assertEquals("Error submitting resource: At least one tag is required.", response.getBody().get("message"));
    }

    // ===================================================================================
    // Offline Resource Functionality Tests
    // ===================================================================================

    @Test
    @DisplayName("UT-RC-20: offlineResource returns success when resource exists and status is valid")
    void offlineResourceReturnsSuccessWhenValid() throws Exception {
        when(resourceService.offlineResource(eq(TEST_RESOURCE_ID), eq(TEST_USER_ID), any()))
                .thenReturn(true);

        mockMvc.perform(put("/api/resources/offline/{id}", TEST_RESOURCE_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"message\":\"Resource unpublished successfully.\"}"));
    }

    @Test
    @DisplayName("UT-RC-21: offlineResource returns success with note")
    void offlineResourceReturnsSuccessWithNote() throws Exception {
        String note = "Reason for offline: content review";

        when(resourceService.offlineResource(eq(TEST_RESOURCE_ID), eq(TEST_USER_ID), eq(note)))
                .thenReturn(true);

        ResourceActionRequestDto requestDto = new ResourceActionRequestDto();
        requestDto.setNote(note);

        mockMvc.perform(put("/api/resources/offline/{id}", TEST_RESOURCE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"message\":\"Resource unpublished successfully.\"}"));
    }

    @Test
    @DisplayName("UT-RC-22: offlineResource returns bad request when resource not found")
    void offlineResourceReturnsBadRequestWhenNotFound() throws Exception {
        when(resourceService.offlineResource(eq(TEST_RESOURCE_ID), eq(TEST_USER_ID), any()))
                .thenReturn(false);

        mockMvc.perform(put("/api/resources/offline/{id}", TEST_RESOURCE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"success\":false,\"message\":\"Resource not found.\"}"));
    }

    @Test
    @DisplayName("UT-RC-23: offlineResource returns conflict when resource status is invalid")
    void offlineResourceReturnsConflictWhenStatusInvalid() throws Exception {
        String errorMessage = "Only approved or archived resources can be unpublished.";

        when(resourceService.offlineResource(eq(TEST_RESOURCE_ID), eq(TEST_USER_ID), any()))
                .thenThrow(new IllegalStateException(errorMessage));

        mockMvc.perform(put("/api/resources/offline/{id}", TEST_RESOURCE_ID))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"success\":false,\"message\":\"" + errorMessage + "\"}"));
    }

    @Test
    @DisplayName("UT-RC-24: offlineResource returns bad request when user not found")
    void offlineResourceReturnsBadRequestWhenUserNotFound() throws Exception {
        when(resourceService.offlineResource(eq(TEST_RESOURCE_ID), eq(TEST_USER_ID), any()))
                .thenThrow(new IllegalArgumentException("Action user not found."));

        mockMvc.perform(put("/api/resources/offline/{id}", TEST_RESOURCE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"success\":false,\"message\":\"Action user not found.\"}"));
    }

    @Test
    @DisplayName("UT-RC-25: offlineResource returns bad request when note exceeds max length")
    void offlineResourceReturnsBadRequestWhenNoteTooLong() throws Exception {
        String longNote = "a".repeat(600); // Exceeds MAX_ACTION_NOTE_LENGTH of 500

        when(resourceService.offlineResource(eq(TEST_RESOURCE_ID), eq(TEST_USER_ID), eq(longNote)))
                .thenThrow(new IllegalArgumentException("note must be 500 characters or fewer."));

        ResourceActionRequestDto requestDto = new ResourceActionRequestDto();
        requestDto.setNote(longNote);

        mockMvc.perform(put("/api/resources/offline/{id}", TEST_RESOURCE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"success\":false,\"message\":\"note must be 500 characters or fewer.\"}"));
    }

    @Test
    @DisplayName("UT-RC-26: offlineResource works without request body")
    void offlineResourceWorksWithoutRequestBody() throws Exception {
        when(resourceService.offlineResource(eq(TEST_RESOURCE_ID), eq(TEST_USER_ID), eq(null)))
                .thenReturn(true);

        mockMvc.perform(put("/api/resources/offline/{id}", TEST_RESOURCE_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"message\":\"Resource unpublished successfully.\"}"));
    }

    private void allowContributorSubmission(User contributor) {
        when(userService.getUserById(TEST_USER_ID)).thenReturn(Optional.of(contributor));
        when(userService.isContributor(contributor)).thenReturn(true);
    }

    private static User contributorUser() {
        User user = new User();
        user.setUserId(TEST_USER_ID);
        user.setEmail("contributor@example.com");
        user.setUsername("Contributor");
        user.setRole(UserRole.CONTRIBUTOR);
        user.setRoleRequestStatus(ContributorRequestStatus.APPROVED);
        return user;
    }

    private static User regularUser() {
        User user = new User();
        user.setUserId(2L);
        user.setEmail("user@example.com");
        user.setUsername("RegularUser");
        user.setRole(UserRole.USER);
        user.setRoleRequestStatus(ContributorRequestStatus.NONE);
        return user;
    }

    private static Resource savedResource() {
        Resource resource = new Resource();
        resource.setResourceId(10L);
        resource.setContributorId(TEST_USER_ID);
        resource.setTitle("Shadow Puppetry");
        resource.setLocation("Xi'an");
        resource.setCategoryId(3L);
        resource.setDescription("Traditional performance archive");
        resource.setCopyrightDeclaration("I own the rights");
        resource.setStatus(ResourceStatus.PENDING_REVIEW);
        return resource;
    }
}
