package com.example.heritage_sharing_api.repository;

import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByStatusOrderBySubmittedAtDesc(ResourceStatus status);

    List<Resource> findByStatusInOrderBySubmittedAtDesc(List<ResourceStatus> statuses);

    List<Resource> findByStatusInAndTitleContainingIgnoreCaseOrderBySubmittedAtDesc(List<ResourceStatus> statuses, String title);

    long countByCategoryId(Long categoryId);

    long countByContributorIdAndStatus(Long contributorId, ResourceStatus status);

    List<Resource> findAllByContributorId(Long contributorId);
    
    List<Resource> findAllByContributorIdOrderBySubmittedAtDesc(Long contributorId);

    List<Resource> findByStatus(ResourceStatus status);

    Optional<Resource> findByResourceIdAndStatus(Long resourceId, ResourceStatus status);
    
    // Optimized: Count resources by contributor ID and multiple statuses in a single query
    // Replaces three separate count() calls with one efficient aggregation query
    // Returns array: [pendingCount, approvedCount, rejectedCount]
    @Query("SELECT " +
           "CAST(COUNT(CASE WHEN r.status = com.example.heritage_sharing_api.entity.ResourceStatus.PENDING_REVIEW THEN 1 END) AS java.lang.Long), " +
           "CAST(COUNT(CASE WHEN r.status = com.example.heritage_sharing_api.entity.ResourceStatus.APPROVED THEN 1 END) AS java.lang.Long), " +
           "CAST(COUNT(CASE WHEN r.status = com.example.heritage_sharing_api.entity.ResourceStatus.REJECTED THEN 1 END) AS java.lang.Long) " +
           "FROM Resource r WHERE r.contributorId = :contributorId")
    Object[] countByContributorIdAndMultipleStatuses(@Param("contributorId") Long contributorId);
}
