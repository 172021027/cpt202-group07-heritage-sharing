package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.dto.ResourceCommentResponse;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceComment;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.repository.ResourceCommentRepository;
import com.example.heritage_sharing_api.repository.ResourceRepository;
import com.example.heritage_sharing_api.repository.UserRepository;
import com.example.heritage_sharing_api.service.ResourceCommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for resource comments")
class ResourceCommentServiceTest {

    @Mock
    private ResourceCommentRepository resourceCommentRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResourceCommentService resourceCommentService;

    private Object[] ownerCommentRow;
    private ResourceComment existingComment;

    @BeforeEach
    void setUpTestData() {
        ownerCommentRow = new Object[] {
                51L,
                101L,
                8L,
                "Comment Owner",
                "This heritage site is very meaningful.",
                LocalDateTime.of(2026, 5, 1, 16, 45),
                false
        };

        existingComment = new ResourceComment();
        existingComment.setCommentId(51L);
        existingComment.setResourceId(101L);
        existingComment.setUserId(8L);
        existingComment.setCommentText("Existing comment");
        existingComment.setCommentedAt(LocalDateTime.of(2026, 5, 1, 16, 45));
    }

    @Test
    @DisplayName("UT-CM-01: comments are loaded with display fields and owner delete permission")
    void commentsAreLoadedWithDisplayFieldsAndOwnerDeletePermission() {
        givenApprovedResourceExists(101L);
        when(resourceCommentRepository.findCommentsWithUserDetailsRawByResourceId(101L))
                .thenReturn(List.<Object[]>of(ownerCommentRow));

        List<ResourceCommentResponse> actual =
                resourceCommentService.getCommentsByResourceId(101L, 8L, false);

        assertEquals(1, actual.size());
        assertEquals(51L, actual.get(0).getCommentId());
        assertEquals(101L, actual.get(0).getResourceId());
        assertEquals(8L, actual.get(0).getUserId());
        assertEquals("Comment Owner", actual.get(0).getUsername());
        assertEquals("This heritage site is very meaningful.", actual.get(0).getCommentText());
        assertEquals("2026-05-01 16:45", actual.get(0).getCommentedAt());
        assertTrue(actual.get(0).isCanDelete());

        verify(resourceRepository).findByResourceIdAndStatus(101L, ResourceStatus.APPROVED);
        verify(resourceCommentRepository).findCommentsWithUserDetailsRawByResourceId(101L);
    }

    @Test
    @DisplayName("UT-CM-02: anonymous viewer can read comments but cannot delete them")
    void anonymousViewerCanReadCommentsButCannotDeleteThem() {
        givenApprovedResourceExists(101L);
        when(resourceCommentRepository.findCommentsWithUserDetailsRawByResourceId(101L))
                .thenReturn(List.<Object[]>of(ownerCommentRow));

        List<ResourceCommentResponse> actual =
                resourceCommentService.getCommentsByResourceId(101L, null, false);

        assertEquals(1, actual.size());
        assertFalse(actual.get(0).isCanDelete());
    }

    @Test
    @DisplayName("UT-CM-03: admin viewer can delete comments even when not the owner")
    void adminViewerCanDeleteCommentsEvenWhenNotOwner() {
        givenApprovedResourceExists(101L);
        when(resourceCommentRepository.findCommentsWithUserDetailsRawByResourceId(101L))
                .thenReturn(List.<Object[]>of(ownerCommentRow));

        List<ResourceCommentResponse> actual =
                resourceCommentService.getCommentsByResourceId(101L, 99L, true);

        assertTrue(actual.get(0).isCanDelete());
    }

    @Test
    @DisplayName("UT-CM-04: missing or non-approved resource comments are rejected before loading comments")
    void missingOrNonApprovedResourceCommentsAreRejectedBeforeLoadingComments() {
        when(resourceRepository.findByResourceIdAndStatus(202L, ResourceStatus.APPROVED))
                .thenReturn(Optional.empty());

        NoSuchElementException actual = assertThrows(NoSuchElementException.class,
                () -> resourceCommentService.getCommentsByResourceId(202L, 8L, false));

        assertEquals("Approved resource not found", actual.getMessage());
        verify(resourceCommentRepository, never()).findCommentsWithUserDetailsRawByResourceId(202L);
    }

    @Test
    @DisplayName("UT-CM-05: adding a comment trims text and returns the saved display response")
    void addingCommentTrimsTextAndReturnsDisplayResponse() {
        givenApprovedResourceExists(101L);
        User user = new User();
        user.setUserId(8L);
        user.setUsername("Comment Owner");
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));
        when(resourceCommentRepository.save(any(ResourceComment.class))).thenAnswer(invocation -> {
            ResourceComment saved = invocation.getArgument(0);
            saved.setCommentId(77L);
            return saved;
        });

        ResourceCommentResponse actual =
                resourceCommentService.addComment(101L, 8L, "   Great resource!   ");

        ArgumentCaptor<ResourceComment> captor = ArgumentCaptor.forClass(ResourceComment.class);
        verify(resourceCommentRepository).save(captor.capture());
        assertEquals(101L, captor.getValue().getResourceId());
        assertEquals(8L, captor.getValue().getUserId());
        assertEquals("Great resource!", captor.getValue().getCommentText());
        assertNotNull(captor.getValue().getCommentedAt());
        assertEquals(77L, actual.getCommentId());
        assertEquals("Comment Owner", actual.getUsername());
        assertEquals("Great resource!", actual.getCommentText());
        assertTrue(actual.isCanDelete());
    }

    @Test
    @DisplayName("UT-CM-06: adding a comment to a missing or non-approved resource fails before saving")
    void addingCommentToMissingOrNonApprovedResourceFailsBeforeSaving() {
        when(resourceRepository.findByResourceIdAndStatus(202L, ResourceStatus.APPROVED))
                .thenReturn(Optional.empty());

        NoSuchElementException actual = assertThrows(NoSuchElementException.class,
                () -> resourceCommentService.addComment(202L, 8L, "Should not be saved"));

        assertEquals("Approved resource not found", actual.getMessage());
        verify(resourceCommentRepository, never()).save(any());
        verify(userRepository, never()).findById(8L);
    }

    @Test
    @DisplayName("UT-CM-07: comment owner can delete own comment")
    void commentOwnerCanDeleteOwnComment() {
        when(resourceCommentRepository.findById(51L)).thenReturn(Optional.of(existingComment));

        resourceCommentService.deleteComment(51L, 8L, false);

        verify(resourceCommentRepository).delete(existingComment);
    }

    @Test
    @DisplayName("UT-CM-08: admin can delete another user's comment")
    void adminCanDeleteAnotherUsersComment() {
        when(resourceCommentRepository.findById(51L)).thenReturn(Optional.of(existingComment));

        resourceCommentService.deleteComment(51L, 99L, true);

        verify(resourceCommentRepository).delete(existingComment);
    }

    @Test
    @DisplayName("UT-CM-09: non-owner normal user cannot delete another user's comment")
    void nonOwnerNormalUserCannotDeleteAnotherUsersComment() {
        when(resourceCommentRepository.findById(51L)).thenReturn(Optional.of(existingComment));

        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> resourceCommentService.deleteComment(51L, 9L, false));

        assertEquals("You are not allowed to delete this comment", actual.getMessage());
        verify(resourceCommentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("UT-CM-10: deleting a missing comment returns a not-found error")
    void deletingMissingCommentReturnsNotFoundError() {
        when(resourceCommentRepository.findById(999L)).thenReturn(Optional.empty());

        NoSuchElementException actual = assertThrows(NoSuchElementException.class,
                () -> resourceCommentService.deleteComment(999L, 8L, true));

        assertEquals("Comment not found", actual.getMessage());
        verify(resourceCommentRepository, never()).delete(any());
    }

    private void givenApprovedResourceExists(Long resourceId) {
        Resource approvedResource = new Resource();
        approvedResource.setResourceId(resourceId);
        approvedResource.setStatus(ResourceStatus.APPROVED);
        when(resourceRepository.findByResourceIdAndStatus(resourceId, ResourceStatus.APPROVED))
                .thenReturn(Optional.of(approvedResource));
    }
}
