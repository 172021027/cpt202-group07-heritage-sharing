package com.example.heritage_sharing_api.integration.review;

import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceActionType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Integration tests for review and resubmission")
public class ReviewAndResubmissionIntegrationTests extends IntegrationTestSupport {

    @Test
    @DisplayName("IT-REV-01: admin can list review detail and approve pending submission")
    public void adminCanListReviewDetailAndApprovePendingSubmission() throws Exception {
        Resource pendingResource = saveResource("Paper Cutting", ResourceStatus.PENDING_REVIEW);
        Tag tag = tagRepository.save(new Tag(null, "craft"));
        resourceTagRepository.save(new ResourceTag(pendingResource.getResourceId(), tag.getTagId()));

        var listResult = mvc.perform(get("/api/admin/reviews/pending")
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var detailResult = mvc.perform(get("/api/admin/reviews/" + pendingResource.getResourceId())
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var decisionResult = mvc.perform(post("/api/admin/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "resourceId", pendingResource.getResourceId(),
                        "submissionId", "resource-" + pendingResource.getResourceId(),
                        "decision", "approve",
                        "note", "Approved for public display."
                )))
                .with(authentication(authenticationFor(adminUser)))).andReturn();

        JsonNode listBody = objectMapper.readTree(listResult.getResponse().getContentAsString());
        JsonNode detailBody = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        JsonNode decisionBody = objectMapper.readTree(decisionResult.getResponse().getContentAsString());
        Resource approvedResource = resourceRepository.findById(pendingResource.getResourceId()).orElseThrow();

        Assertions.assertEquals(200, listResult.getResponse().getStatus());
        Assertions.assertEquals(1, listBody.get("totalElements").asInt());
        Assertions.assertEquals("Paper Cutting", listBody.get("items").get(0).get("title").asText());
        Assertions.assertEquals(200, detailResult.getResponse().getStatus());
        Assertions.assertEquals("Contributor", detailBody.get("contributorUsername").asText());
        Assertions.assertEquals(1, detailBody.get("tags").size());
        Assertions.assertEquals(200, decisionResult.getResponse().getStatus());
        Assertions.assertEquals("approved", decisionBody.get("status").asText());
        Assertions.assertEquals(ResourceStatus.APPROVED, approvedResource.getStatus());
        Assertions.assertEquals(ResourceActionType.APPROVE, resourceActionRepository.findByResourceIdOrderByActionAtDesc(pendingResource.getResourceId()).get(0).getActionType());
    }

    @Test
    @DisplayName("IT-REV-02: admin can reject and contributor can read feedback then resubmit")
    public void adminCanRejectSubmissionAndContributorCanReadFeedbackThenResubmit() throws Exception {
        Resource pendingResource = saveResource("Old Embroidery", ResourceStatus.PENDING_REVIEW);

        var rejectResult = mvc.perform(post("/api/admin/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "resourceId", pendingResource.getResourceId(),
                        "submissionId", "resource-" + pendingResource.getResourceId(),
                        "decision", "reject",
                        "note", "Please provide clearer cultural context."
                )))
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var editResult = mvc.perform(get("/api/users/current/submissions/" + pendingResource.getResourceId())
                .with(authentication(authenticationFor(contributorUser)))).andReturn();
        var resubmitResult = mvc.perform(multipart("/api/users/current/submissions/" + pendingResource.getResourceId() + "/resubmit")
                .param("title", "Updated Embroidery")
                .param("location", "Suzhou")
                .param("categoryId", category.getCategoryId().toString())
                .param("description", "Updated embroidery archive")
                .param("tags", "embroidery", "craft")
                .param("copyrightDeclaration", "I own the rights")
                .with(authentication(authenticationFor(contributorUser)))).andReturn();

        JsonNode editBody = objectMapper.readTree(editResult.getResponse().getContentAsString());
        Resource updatedResource = resourceRepository.findById(pendingResource.getResourceId()).orElseThrow();

        Assertions.assertEquals(200, rejectResult.getResponse().getStatus());
        Assertions.assertEquals(200, editResult.getResponse().getStatus());
        Assertions.assertEquals("Please provide clearer cultural context.", editBody.get("feedback").asText());
        Assertions.assertEquals(200, resubmitResult.getResponse().getStatus());
        Assertions.assertEquals("Updated Embroidery", updatedResource.getTitle());
        Assertions.assertEquals(ResourceStatus.PENDING_REVIEW, updatedResource.getStatus());
        Assertions.assertEquals(2, resourceTagRepository.findByResourceId(updatedResource.getResourceId()).size());
    }

    @Test
    @DisplayName("IT-REV-03: review decision rejects invalid states and required field errors")
    public void reviewDecisionRejectsInvalidStatesAndMissingRequiredFields() throws Exception {
        Resource approvedResource = saveResource("Already Approved", ResourceStatus.APPROVED);

        var conflictResult = mvc.perform(post("/api/admin/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "resourceId", approvedResource.getResourceId(),
                        "decision", "approve",
                        "note", "Cannot approve twice"
                )))
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var badDecisionResult = mvc.perform(post("/api/admin/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "resourceId", approvedResource.getResourceId(),
                        "decision", "maybe",
                        "note", "Invalid decision"
                )))
                .with(authentication(authenticationFor(adminUser)))).andReturn();

        Assertions.assertEquals(409, conflictResult.getResponse().getStatus());
        Assertions.assertEquals(400, badDecisionResult.getResponse().getStatus());
    }
}
