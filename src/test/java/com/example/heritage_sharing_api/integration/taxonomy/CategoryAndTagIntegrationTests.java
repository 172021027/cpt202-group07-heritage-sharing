package com.example.heritage_sharing_api.integration.taxonomy;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@DisplayName("Integration tests for category and tag taxonomy")
public class CategoryAndTagIntegrationTests extends IntegrationTestSupport {

    @Test
    @DisplayName("IT-TAX-01: category APIs create read update and delete data")
    public void categoryApisCreateReadUpdateAndDeleteDataThroughServiceAndRepository() throws Exception {
        var createResult = mvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("categoryName", "Traditional Music")))
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        Long categoryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("categoryId").asLong();
        var readResult = mvc.perform(get("/api/categories/" + categoryId)
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var updateResult = mvc.perform(put("/api/categories/" + categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("categoryName", "Updated Music")))
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var listResult = mvc.perform(get("/api/categories")
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var deleteResult = mvc.perform(delete("/api/categories/" + categoryId)
                .with(authentication(authenticationFor(adminUser)))).andReturn();

        Assertions.assertEquals(200, createResult.getResponse().getStatus());
        Assertions.assertEquals(200, readResult.getResponse().getStatus());
        Assertions.assertEquals(200, updateResult.getResponse().getStatus());
        Assertions.assertEquals(true, listResult.getResponse().getContentAsString().contains("Updated Music"));
        Assertions.assertEquals(200, deleteResult.getResponse().getStatus());
        Assertions.assertEquals(false, categoryRepository.existsById(categoryId));
    }

    @Test
    @DisplayName("IT-TAX-02: tag APIs create read update delete and merge unused tags")
    public void tagApisCreateReadUpdateDeleteAndMergeUnusedTags() throws Exception {
        Tag primaryTag = tagRepository.save(new Tag(null, "primary"));
        Tag secondaryTag = tagRepository.save(new Tag(null, "secondary"));
        Resource taggedResource = saveResource("Secondary Tagged Resource", ResourceStatus.APPROVED);
        resourceTagRepository.save(new ResourceTag(taggedResource.getResourceId(), secondaryTag.getTagId()));

        var createResult = mvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("tagName", "music")))
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        Long createdTagId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("tagId").asLong();
        var updateResult = mvc.perform(put("/api/tags/" + createdTagId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("tagName", "updated-music")))
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var mergeResult = mvc.perform(post("/api/tags/merge")
                .param("secondaryTagId", secondaryTag.getTagId().toString())
                .param("primaryTagId", primaryTag.getTagId().toString())
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var listResult = mvc.perform(get("/api/tags")
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var deleteResult = mvc.perform(delete("/api/tags/" + createdTagId)
                .with(authentication(authenticationFor(adminUser)))).andReturn();

        JsonNode listBody = objectMapper.readTree(listResult.getResponse().getContentAsString());

        Assertions.assertEquals(200, createResult.getResponse().getStatus());
        Assertions.assertEquals(200, updateResult.getResponse().getStatus());
        Assertions.assertEquals(200, mergeResult.getResponse().getStatus());
        Assertions.assertEquals(false, tagRepository.existsById(secondaryTag.getTagId()));
        Assertions.assertEquals(false, resourceTagRepository.existsByResourceIdAndTagId(taggedResource.getResourceId(), secondaryTag.getTagId()));
        Assertions.assertEquals(true, resourceTagRepository.existsByResourceIdAndTagId(taggedResource.getResourceId(), primaryTag.getTagId()));
        Assertions.assertEquals(true, listBody.size() >= 2);
        Assertions.assertEquals(200, deleteResult.getResponse().getStatus());
    }

    @Test
    @DisplayName("IT-TAX-03: merging a tag with itself returns bad request")
    public void mergeTagWithItselfReturnsBadRequest() throws Exception {
        Tag tag = tagRepository.save(new Tag(null, "duplicate"));

        var result = mvc.perform(post("/api/tags/merge")
                .param("secondaryTagId", tag.getTagId().toString())
                .param("primaryTagId", tag.getTagId().toString())
                .with(authentication(authenticationFor(adminUser)))).andReturn();

        Assertions.assertEquals(400, result.getResponse().getStatus());
        Assertions.assertEquals(true, tagRepository.existsById(tag.getTagId()));
    }

    @Test
    @DisplayName("IT-TAX-04: taxonomy redirect endpoints return admin taxonomy location")
    public void taxonomyRedirectEndpointsReturnAdminTaxonomyLocation() throws Exception {
        var adminRedirect = mvc.perform(get("/api/taxonomy/admin")
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var rootRedirect = mvc.perform(get("/api/taxonomy/")
                .with(authentication(authenticationFor(adminUser)))).andReturn();

        Assertions.assertEquals(302, adminRedirect.getResponse().getStatus());
        Assertions.assertEquals("/html/admin/taxonomy.html", adminRedirect.getResponse().getRedirectedUrl());
        Assertions.assertEquals(302, rootRedirect.getResponse().getStatus());
        Assertions.assertEquals("/html/admin/taxonomy.html", rootRedirect.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("IT-TAX-05: regular user can read taxonomy but cannot mutate it")
    public void regularUserCanReadTaxonomyButCannotMutateIt() throws Exception {
        Tag tag = tagRepository.save(new Tag(null, "readonly"));

        var categoryListResult = mvc.perform(get("/api/categories")
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        var tagListResult = mvc.perform(get("/api/tags")
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        var createCategoryResult = mvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("categoryName", "Forbidden Category")))
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        var updateTagResult = mvc.perform(put("/api/tags/" + tag.getTagId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("tagName", "forbidden-update")))
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        var deleteTagResult = mvc.perform(delete("/api/tags/" + tag.getTagId())
                .with(authentication(authenticationFor(regularUser)))).andReturn();
        var mergeTagsResult = mvc.perform(post("/api/tags/merge?secondaryTagId={secondaryTagId}&primaryTagId={primaryTagId}",
                        tag.getTagId(), tag.getTagId())
                .with(authentication(authenticationFor(regularUser)))).andReturn();

        Assertions.assertEquals(200, categoryListResult.getResponse().getStatus());
        Assertions.assertEquals(200, tagListResult.getResponse().getStatus());
        Assertions.assertEquals(403, createCategoryResult.getResponse().getStatus());
        Assertions.assertEquals(403, updateTagResult.getResponse().getStatus());
        Assertions.assertEquals(403, deleteTagResult.getResponse().getStatus());
        Assertions.assertEquals(403, mergeTagsResult.getResponse().getStatus());
        Assertions.assertEquals(true, tagRepository.existsById(tag.getTagId()));
    }
}
