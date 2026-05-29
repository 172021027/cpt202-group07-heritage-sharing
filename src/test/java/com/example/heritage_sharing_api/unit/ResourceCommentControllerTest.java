package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.controller.ResourceCommentController;
import com.example.heritage_sharing_api.dto.ResourceCommentRequest;
import com.example.heritage_sharing_api.dto.ResourceCommentResponse;
import com.example.heritage_sharing_api.service.ResourceCommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for ResourceCommentController")
class ResourceCommentControllerTest {

    @Mock
    private ResourceCommentService resourceCommentService;

    @InjectMocks
    private ResourceCommentController resourceCommentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(resourceCommentController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("UT-RCC-01: getCommentsByResourceId returns comments for anonymous viewer")
    void getCommentsByResourceIdReturnsCommentsForAnonymousViewer() throws Exception {
        ResourceCommentResponse comment = commentResponse(false);
        when(resourceCommentService.getCommentsByResourceId(101L, null, false)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/resource-comments/resource/{resourceId}", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(51))
                .andExpect(jsonPath("$[0].resourceId").value(101))
                .andExpect(jsonPath("$[0].username").value("Comment Owner"))
                .andExpect(jsonPath("$[0].canDelete").value(false));

        verify(resourceCommentService).getCommentsByResourceId(101L, null, false);
    }

    @Test
    @DisplayName("UT-RCC-02: getCommentsByResourceId forwards authenticated admin context")
    void getCommentsByResourceIdForwardsAuthenticatedAdminContext() throws Exception {
        ResourceCommentResponse comment = commentResponse(true);
        when(resourceCommentService.getCommentsByResourceId(101L, 9L, true)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/resource-comments/resource/{resourceId}", 101L)
                        .principal(authenticatedUser(9L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].canDelete").value(true));

        verify(resourceCommentService).getCommentsByResourceId(101L, 9L, true);
    }

    @Test
    @DisplayName("UT-RCC-03: getCommentsByResourceId returns 404 when resource is not found")
    void getCommentsByResourceIdReturnsNotFoundWhenResourceMissing() throws Exception {
        when(resourceCommentService.getCommentsByResourceId(999L, null, false))
                .thenThrow(new NoSuchElementException("Approved resource not found"));

        mockMvc.perform(get("/api/resource-comments/resource/{resourceId}", 999L))
                .andExpect(status().isNotFound());

        verify(resourceCommentService).getCommentsByResourceId(999L, null, false);
    }

    @Test
    @DisplayName("UT-RCC-04: addComment returns saved comment when authenticated")
    void addCommentReturnsSavedCommentWhenAuthenticated() throws Exception {
        ResourceCommentRequest request = new ResourceCommentRequest();
        request.setResourceId(101L);
        request.setCommentText("Great resource!");
        ResourceCommentResponse response = commentResponse(true);
        response.setCommentText("Great resource!");
        when(resourceCommentService.addComment(101L, 8L, "Great resource!")).thenReturn(response);

        mockMvc.perform(post("/api/resource-comments/add")
                        .principal(authenticatedUser(8L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(51))
                .andExpect(jsonPath("$.commentText").value("Great resource!"))
                .andExpect(jsonPath("$.canDelete").value(true));

        verify(resourceCommentService).addComment(101L, 8L, "Great resource!");
    }

    @Test
    @DisplayName("UT-RCC-05: addComment returns 401 when authentication is missing")
    void addCommentReturnsUnauthorizedWhenAuthenticationMissing() throws Exception {
        ResourceCommentRequest request = new ResourceCommentRequest();
        request.setResourceId(101L);
        request.setCommentText("Great resource!");

        mockMvc.perform(post("/api/resource-comments/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please log in to comment"));

        verify(resourceCommentService, never()).addComment(101L, null, "Great resource!");
    }

    @Test
    @DisplayName("UT-RCC-06: addComment returns 400 when content is empty")
    void addCommentReturnsBadRequestWhenContentEmpty() throws Exception {
        ResourceCommentRequest request = new ResourceCommentRequest();
        request.setResourceId(101L);
        request.setCommentText("   ");

        mockMvc.perform(post("/api/resource-comments/add")
                        .principal(authenticatedUser(8L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Comment content cannot be empty"));

        verify(resourceCommentService, never()).addComment(101L, 8L, "   ");
    }

    @Test
    @DisplayName("UT-RCC-07: deleteComment returns success for owner")
    void deleteCommentReturnsSuccessForOwner() throws Exception {
        mockMvc.perform(delete("/api/resource-comments/{commentId}", 51L)
                        .principal(authenticatedUser(8L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment deleted successfully"));

        verify(resourceCommentService).deleteComment(51L, 8L, false);
    }

    @Test
    @DisplayName("UT-RCC-08: deleteComment returns 403 when service denies access")
    void deleteCommentReturnsForbiddenWhenServiceDeniesAccess() throws Exception {
        doThrow(new IllegalStateException("You are not allowed to delete this comment"))
                .when(resourceCommentService).deleteComment(51L, 9L, false);

        mockMvc.perform(delete("/api/resource-comments/{commentId}", 51L)
                        .principal(authenticatedUser("9")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to delete this comment"));

        verify(resourceCommentService).deleteComment(51L, 9L, false);
    }

    private static ResourceCommentResponse commentResponse(boolean canDelete) {
        return new ResourceCommentResponse(
                51L,
                101L,
                8L,
                "Comment Owner",
                "This heritage site is very meaningful.",
                "2026-05-01 16:45",
                canDelete);
    }

    private static Authentication authenticatedUser(Object principal, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities);
    }
}
