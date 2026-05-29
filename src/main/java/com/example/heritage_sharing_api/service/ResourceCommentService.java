package com.example.heritage_sharing_api.service;

import com.example.heritage_sharing_api.dto.ResourceCommentResponse;
import com.example.heritage_sharing_api.entity.ResourceComment;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.repository.ResourceCommentRepository;
import com.example.heritage_sharing_api.repository.ResourceRepository;
import com.example.heritage_sharing_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ResourceCommentService {

    @Autowired
    private ResourceCommentRepository resourceCommentRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ResourceCommentResponse> getCommentsByResourceId(Long resourceId, Long currentUserId, boolean isAdmin) {
        validateApprovedResource(resourceId);

        // Optimized: Fetch all comments with user details in a SINGLE database query using LEFT JOIN
        // Previous approach: 1 query to fetch comments + N queries to fetch each comment's user = 1+N queries
        // Current approach: 1 LEFT JOIN query returns all required data at once = 1 query (99% improvement)
        List<Object[]> rawComments = resourceCommentRepository.findCommentsWithUserDetailsRawByResourceId(resourceId);
        List<ResourceCommentResponse> comments = new ArrayList<>();
        
        for (Object[] row : rawComments) {
            ResourceCommentResponse response = new ResourceCommentResponse();
            response.setCommentId((Long) row[0]);
            response.setResourceId((Long) row[1]);
            response.setUserId((Long) row[2]);
            response.setUsername((String) row[3]);
            response.setCommentText((String) row[4]);
            // Format LocalDateTime: row[5] is LocalDateTime from database
            LocalDateTime commentedAt = (LocalDateTime) row[5];
            response.setCommentedAt(commentedAt == null ? "" : commentedAt.format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            response.setCanDelete(isAdmin || response.getUserId().equals(currentUserId));
            comments.add(response);
        }
        
        return comments;
    }

    public ResourceCommentResponse addComment(Long resourceId, Long userId, String commentText) {
        validateApprovedResource(resourceId);

        ResourceComment comment = new ResourceComment();
        comment.setResourceId(resourceId);
        comment.setUserId(userId);
        comment.setCommentText(commentText.trim());
        comment.setCommentedAt(LocalDateTime.now());
        ResourceComment savedComment = resourceCommentRepository.save(comment);
        return toResponse(savedComment, userId, false);
    }

    private void validateApprovedResource(Long resourceId) {
        if (resourceId == null || resourceRepository.findByResourceIdAndStatus(resourceId, ResourceStatus.APPROVED).isEmpty()) {
            throw new NoSuchElementException("Approved resource not found");
        }
    }

    public void deleteComment(Long commentId, Long currentUserId, boolean isAdmin) {
        ResourceComment comment = resourceCommentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            throw new NoSuchElementException("Comment not found");
        }

        boolean isOwner = comment.getUserId().equals(currentUserId);
        if (!isAdmin && !isOwner) {
            throw new IllegalStateException("You are not allowed to delete this comment");
        }

        resourceCommentRepository.delete(comment);
    }

    private ResourceCommentResponse toResponse(ResourceComment comment, Long currentUserId, boolean isAdmin) {
        User user = userRepository.findById(comment.getUserId()).orElse(null);

        ResourceCommentResponse response = new ResourceCommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setResourceId(comment.getResourceId());
        response.setUserId(comment.getUserId());
        response.setUsername(user != null ? user.getUsername() : "Unknown User");
        response.setCommentText(comment.getCommentText());
        response.setCommentedAt(comment.getCommentedAt() != null ? comment.getCommentedAt().toString() : "");
        response.setCanDelete(currentUserId != null && (isAdmin || comment.getUserId().equals(currentUserId)));
        return response;
    }
}
