package com.example.heritage_sharing_api.repository;

import com.example.heritage_sharing_api.dto.ResourceActionResponse;
import com.example.heritage_sharing_api.entity.ResourceAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceActionRepository extends JpaRepository<ResourceAction, Long> {
    List<ResourceAction> findByResourceIdOrderByActionAtDesc(Long resourceId);

    List<ResourceAction> findByResourceIdOrderByActionAtDescActionIdDesc(Long resourceId);

    // Retrieve all resource actions, ordered by time descending with pagination
    Page<ResourceAction> findAllByOrderByActionAtDescActionIdDesc(Pageable pageable);

    // Use LEFT JOIN to fetch action, resource, and user details in one query.
    @Query("SELECT new com.example.heritage_sharing_api.dto.ResourceActionResponse(" +
           "a.actionId, a.resourceId, COALESCE(r.title, 'Unknown Resource'), " +
           "a.actionType, a.actionByUserId, COALESCE(u.username, 'Unknown User'), " +
           "a.actionAt, COALESCE(a.feedbackText, '')) " +
           "FROM ResourceAction a " +
           "LEFT JOIN Resource r ON a.resourceId = r.resourceId " +
           "LEFT JOIN User u ON a.actionByUserId = u.userId " +
           "ORDER BY a.actionAt DESC, a.actionId DESC")
    Page<ResourceActionResponse> findAllWithDetails(Pageable pageable);
}
