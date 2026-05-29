package com.example.heritage_sharing_api.repository;

import com.example.heritage_sharing_api.entity.ResourceComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceCommentRepository extends JpaRepository<ResourceComment, Long> {
    List<ResourceComment> findByResourceIdOrderByCommentedAtDesc(Long resourceId);

    // Optimized: Single LEFT JOIN query to fetch comments with user details
    // Eliminates N+1 query pattern by fetching all user info in one database call
    // Note: Date formatting done in service layer due to JPQL limitations
    @Query("SELECT c.commentId, c.resourceId, c.userId, " +
           "COALESCE(u.username, 'Unknown User'), " +
           "c.commentText, c.commentedAt, false " +
           "FROM ResourceComment c " +
           "LEFT JOIN User u ON c.userId = u.userId " +
           "WHERE c.resourceId = :resourceId " +
           "ORDER BY c.commentedAt DESC")
    List<Object[]> findCommentsWithUserDetailsRawByResourceId(@Param("resourceId") Long resourceId);
}
