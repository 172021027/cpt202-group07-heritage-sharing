package com.example.heritage_sharing_api.repository;

import com.example.heritage_sharing_api.entity.ResourceTag;
import com.example.heritage_sharing_api.entity.ResourceTagId;
import com.example.heritage_sharing_api.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ResourceTagRepository extends JpaRepository<ResourceTag, ResourceTagId> {
    List<ResourceTag> findByTagId(Long tagId);
    List<ResourceTag> findByResourceId(Long resourceId);
    boolean existsByResourceIdAndTagId(Long resourceId, Long tagId);
    void deleteByResourceId(Long resourceId);
    
    // Optimized: Fetch all resource-tag associations for multiple resources in one query
    // Eliminates N+1 pattern when resolving tags for multiple resources
    @Query("SELECT rt FROM ResourceTag rt WHERE rt.resourceId IN :resourceIds")
    List<ResourceTag> findByResourceIdIn(@Param("resourceIds") Set<Long> resourceIds);
    
    // Optimized: Fetch all tags for a resource directly with JOIN to tags table
    // Combines two queries (findByResourceId + findAllById) into one efficient LEFT JOIN query
    @Query("SELECT t FROM Tag t " +
           "INNER JOIN ResourceTag rt ON t.tagId = rt.tagId " +
           "WHERE rt.resourceId = :resourceId " +
           "ORDER BY t.tagName")
    List<Tag> findTagsByResourceId(@Param("resourceId") Long resourceId);
}
