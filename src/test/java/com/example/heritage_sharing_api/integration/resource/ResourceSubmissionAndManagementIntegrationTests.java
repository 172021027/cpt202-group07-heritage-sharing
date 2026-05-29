package com.example.heritage_sharing_api.integration.resource;

import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.ResourceTag;
import com.example.heritage_sharing_api.entity.Tag;
import com.example.heritage_sharing_api.integration.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@DisplayName("Integration tests for resource submission and management")
public class ResourceSubmissionAndManagementIntegrationTests extends IntegrationTestSupport {

    @Test
    @DisplayName("IT-RES-01: contributor submission persists pending resource and normalized tags")
    public void contributorSubmissionPersistsPendingResourceAndNormalizedTags() throws Exception {
        var result = mvc.perform(multipart("/api/resources/submit")
                .param("title", "Shadow Puppetry")
                .param("location", "Xi'an")
                .param("categoryId", category.getCategoryId().toString())
                .param("description", "Traditional shadow puppet performance archive")
                .param("tags", "performance", "intangible", "performance")
                .param("copyrightDeclaration", "I own the rights")
                .with(authentication(authenticationFor(contributorUser)))).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Resource savedResource = resourceRepository.findById(body.get("resourceId").asLong()).orElseThrow();

        Assertions.assertEquals(200, result.getResponse().getStatus());
        Assertions.assertEquals(ResourceStatus.PENDING_REVIEW, savedResource.getStatus());
        Assertions.assertEquals(contributorUser.getUserId(), savedResource.getContributorId());
        Assertions.assertEquals(2, resourceTagRepository.findByResourceId(savedResource.getResourceId()).size());
    }

    @Test
    @DisplayName("IT-RES-02: non-contributor and anonymous users cannot submit resources")
    public void nonContributorAndAnonymousUsersCannotSubmitResources() throws Exception {
        var regularResult = mvc.perform(multipart("/api/resources/submit")
                .param("title", "Blocked Submission")
                .param("location", "Xi'an")
                .param("categoryId", category.getCategoryId().toString())
                .param("description", "Should not save")
                .param("tags", "blocked")
                .param("copyrightDeclaration", "I own the rights")
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        var anonymousResult = mvc.perform(multipart("/api/resources/submit")
                .param("title", "Anonymous Submission")
                .param("location", "Xi'an")
                .param("categoryId", category.getCategoryId().toString())
                .param("description", "Should not save")
                .param("tags", "blocked")
                .param("copyrightDeclaration", "I own the rights")).andReturn();

        Assertions.assertEquals(403, regularResult.getResponse().getStatus());
        Assertions.assertEquals(403, anonymousResult.getResponse().getStatus());
        Assertions.assertEquals(0, resourceRepository.count());
    }

    @Test
    @DisplayName("IT-RES-03: public endpoints expose only approved resources and apply filters")
    public void publicEndpointsOnlyExposeApprovedResourcesAndApplySearchFilterCriteria() throws Exception {
        Resource approvedResource = saveResource("Dragon Dance", ResourceStatus.APPROVED);
        Resource pendingResource = saveResource("Hidden Draft", ResourceStatus.PENDING_REVIEW);
        Tag festivalTag = tagRepository.save(new Tag(null, "festival"));
        resourceTagRepository.save(new ResourceTag(approvedResource.getResourceId(), festivalTag.getTagId()));
        resourceTagRepository.save(new ResourceTag(pendingResource.getResourceId(), festivalTag.getTagId()));

        var approvedResult = mvc.perform(get("/api/resources/approved")).andReturn();
        var detailResult = mvc.perform(get("/api/resources/" + approvedResource.getResourceId())).andReturn();
        var pendingDetailResult = mvc.perform(get("/api/resources/" + pendingResource.getResourceId())).andReturn();
        var searchResult = mvc.perform(get("/api/resources/search").param("keyword", "dragon")).andReturn();
        var filterResult = mvc.perform(get("/api/resources/search-and-filter")
                .param("keyword", "dance")
                .param("categoryId", category.getCategoryId().toString())
                .param("tag", "festival")
                .param("location", "xi")).andReturn();

        Assertions.assertEquals(200, approvedResult.getResponse().getStatus());
        Assertions.assertEquals(true, approvedResult.getResponse().getContentAsString().contains("Dragon Dance"));
        Assertions.assertEquals(false, approvedResult.getResponse().getContentAsString().contains("Hidden Draft"));
        Assertions.assertEquals(200, detailResult.getResponse().getStatus());
        Assertions.assertEquals(404, pendingDetailResult.getResponse().getStatus());
        Assertions.assertEquals(1, objectMapper.readTree(searchResult.getResponse().getContentAsString()).size());
        Assertions.assertEquals(1, objectMapper.readTree(filterResult.getResponse().getContentAsString()).size());
    }

    @Test
    @DisplayName("IT-RES-04: admin lifecycle actions update status and action history together")
    public void adminResourceLifecycleActionsUpdateStatusAndActionHistoryTogether() throws Exception {
        Resource approvedResource = saveResource("Folk Song", ResourceStatus.APPROVED);

        var archiveResult = mvc.perform(put("/api/resources/archive/" + approvedResource.getResourceId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("note", "Seasonal archive")))
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var restoreResult = mvc.perform(put("/api/resources/restore/" + approvedResource.getResourceId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("note", "Ready again")))
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var offlineResult = mvc.perform(put("/api/resources/offline/" + approvedResource.getResourceId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("note", "Copyright review required")))
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var historyResult = mvc.perform(get("/api/resources/actions/history")
                .with(authentication(authenticationFor(adminUser)))).andReturn();

        Resource updatedResource = resourceRepository.findById(approvedResource.getResourceId()).orElseThrow();
        JsonNode historyBody = objectMapper.readTree(historyResult.getResponse().getContentAsString());

        Assertions.assertEquals(200, archiveResult.getResponse().getStatus());
        Assertions.assertEquals(200, restoreResult.getResponse().getStatus());
        Assertions.assertEquals(200, offlineResult.getResponse().getStatus());
        Assertions.assertEquals(ResourceStatus.UNPUBLISHED, updatedResource.getStatus());
        Assertions.assertEquals(3, resourceActionRepository.findByResourceIdOrderByActionAtDesc(approvedResource.getResourceId()).size());
        Assertions.assertEquals(3, historyBody.get("totalElements").asInt());
    }

    @Test
    @DisplayName("IT-RES-05: contributor can soft delete own resource but not another user's")
    public void contributorCanSoftDeleteOwnResourceButNotAnotherUsersResource() throws Exception {
        Resource ownResource = saveResource("Own Submission", ResourceStatus.PENDING_REVIEW);
        Resource otherResource = resourceRepository.save(buildResource(otherUser.getUserId(), "Other Submission", ResourceStatus.PENDING_REVIEW));

        var forbiddenResult = mvc.perform(delete("/api/resources/mine/" + otherResource.getResourceId())
                .with(authentication(authenticationFor(contributorUser)))).andReturn();
        var deleteResult = mvc.perform(delete("/api/resources/mine/" + ownResource.getResourceId())
                .with(authentication(authenticationFor(contributorUser)))).andReturn();

        Assertions.assertEquals(403, forbiddenResult.getResponse().getStatus());
        Assertions.assertEquals(200, deleteResult.getResponse().getStatus());
        Assertions.assertEquals(ResourceStatus.DELETED, resourceRepository.findById(ownResource.getResourceId()).orElseThrow().getStatus());
    }
}
