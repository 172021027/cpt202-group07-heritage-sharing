package com.example.heritage_sharing_api.integration.comment;

import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceComment;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.integration.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Integration tests for resource comments")
public class ResourceCommentsIntegrationTests extends IntegrationTestSupport {

    @Test
    @DisplayName("IT-COM-01: approved resource comments can be added listed and deleted by owner")
    public void approvedResourceCommentsCanBeAddedListedAndDeletedWithOwnershipRules() throws Exception {
        Resource approvedResource = saveResource("Lion Dance", ResourceStatus.APPROVED);

        var addResult = mvc.perform(post("/api/resource-comments/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "resourceId", approvedResource.getResourceId(),
                        "commentText", "Wonderful archive."
                )))
                .with(authentication(authenticationFor(otherUser)))).andReturn();
        JsonNode addBody = objectMapper.readTree(addResult.getResponse().getContentAsString());
        Long commentId = addBody.get("commentId").asLong();
        long countAfterAdd = resourceCommentRepository.count();

        var listResult = mvc.perform(get("/api/resource-comments/resource/" + approvedResource.getResourceId())
                .with(authentication(authenticationFor(otherUser)))).andReturn();
        var forbiddenDeleteResult = mvc.perform(delete("/api/resource-comments/" + commentId)
                .with(authentication(authenticationFor(contributorUser)))).andReturn();
        long countAfterForbiddenDelete = resourceCommentRepository.count();
        var ownerDeleteResult = mvc.perform(delete("/api/resource-comments/" + commentId)
                .with(authentication(authenticationFor(otherUser)))).andReturn();

        JsonNode listBody = objectMapper.readTree(listResult.getResponse().getContentAsString());

        Assertions.assertEquals(200, addResult.getResponse().getStatus());
        Assertions.assertEquals(1, countAfterAdd);
        Assertions.assertEquals(200, listResult.getResponse().getStatus());
        Assertions.assertEquals(true, listBody.get(0).get("canDelete").asBoolean());
        Assertions.assertEquals(403, forbiddenDeleteResult.getResponse().getStatus());
        Assertions.assertEquals(1, countAfterForbiddenDelete);
        Assertions.assertEquals(200, ownerDeleteResult.getResponse().getStatus());
        Assertions.assertEquals(0, resourceCommentRepository.count());
    }

    @Test
    @DisplayName("IT-COM-02: admin can delete any comment and anonymous user cannot add comment")
    public void adminCanDeleteAnyCommentAndAnonymousCannotAddComment() throws Exception {
        Resource approvedResource = saveResource("Approved Archive", ResourceStatus.APPROVED);
        ResourceComment comment = new ResourceComment();
        comment.setResourceId(approvedResource.getResourceId());
        comment.setUserId(otherUser.getUserId());
        comment.setCommentText("Needs admin moderation");
        comment.setCommentedAt(LocalDateTime.now());
        comment = resourceCommentRepository.save(comment);

        var anonymousAddResult = mvc.perform(post("/api/resource-comments/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "resourceId", approvedResource.getResourceId(),
                        "commentText", "Anonymous comment"
                )))).andReturn();
        var adminDeleteResult = mvc.perform(delete("/api/resource-comments/" + comment.getCommentId())
                .with(authentication(authenticationFor(adminUser)))).andReturn();

        Assertions.assertEquals(403, anonymousAddResult.getResponse().getStatus());
        Assertions.assertEquals(200, adminDeleteResult.getResponse().getStatus());
        Assertions.assertEquals(0, resourceCommentRepository.count());
    }

    @Test
    @DisplayName("IT-COM-03: comments cannot be added or listed for non-approved resources")
    public void commentsCannotBeAddedOrListedForNonApprovedResources() throws Exception {
        Resource pendingResource = saveResource("Pending Archive", ResourceStatus.PENDING_REVIEW);

        var addResult = mvc.perform(post("/api/resource-comments/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "resourceId", pendingResource.getResourceId(),
                        "commentText", "Not public yet"
                )))
                .with(authentication(authenticationFor(otherUser)))).andReturn();
        var listResult = mvc.perform(get("/api/resource-comments/resource/" + pendingResource.getResourceId())
                .with(authentication(authenticationFor(otherUser)))).andReturn();

        Assertions.assertEquals(404, addResult.getResponse().getStatus());
        Assertions.assertEquals(404, listResult.getResponse().getStatus());
    }
}
