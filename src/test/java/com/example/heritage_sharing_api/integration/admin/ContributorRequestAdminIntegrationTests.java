package com.example.heritage_sharing_api.integration.admin;

import com.example.heritage_sharing_api.entity.ContributorRequestStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.integration.support.IntegrationTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Integration tests for admin contributor requests")
public class ContributorRequestAdminIntegrationTests extends IntegrationTestSupport {

    @Test
    @DisplayName("IT-ADM-01: admin can list and approve pending contributor request")
    public void adminCanListAndApprovePendingContributorRequest() throws Exception {
        User applicant = savePendingContributorApplicant("Applicant", "applicant.integration@example.com");

        var listResult = mvc.perform(get("/api/admin/contributor-requests")
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var approveResult = mvc.perform(post("/api/admin/contributor-requests/" + applicant.getUserId() + "/approve")
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        User approvedUser = userRepository.findById(applicant.getUserId()).orElseThrow();

        Assertions.assertEquals(200, listResult.getResponse().getStatus());
        Assertions.assertEquals(true, listResult.getResponse().getContentAsString().contains("Applicant"));
        Assertions.assertEquals(200, approveResult.getResponse().getStatus());
        Assertions.assertEquals(UserRole.CONTRIBUTOR, approvedUser.getRole());
        Assertions.assertEquals(ContributorRequestStatus.APPROVED, approvedUser.getRoleRequestStatus());
    }

    @Test
    @DisplayName("IT-ADM-02: admin can reject pending contributor request")
    public void adminCanRejectPendingContributorRequest() throws Exception {
        User applicant = savePendingContributorApplicant("RejectedApplicant", "rejected.applicant@example.com");

        var rejectResult = mvc.perform(post("/api/admin/contributor-requests/" + applicant.getUserId() + "/reject")
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        User rejectedUser = userRepository.findById(applicant.getUserId()).orElseThrow();

        Assertions.assertEquals(200, rejectResult.getResponse().getStatus());
        Assertions.assertEquals(UserRole.USER, rejectedUser.getRole());
        Assertions.assertEquals(ContributorRequestStatus.REJECTED, rejectedUser.getRoleRequestStatus());
    }

    @Test
    @DisplayName("IT-ADM-03: admin can list and revoke approved contributor")
    public void adminCanListAndRevokeApprovedContributor() throws Exception {
        var contributorsResult = mvc.perform(get("/api/admin/contributor-requests/contributors")
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        var revokeResult = mvc.perform(post("/api/admin/contributor-requests/" + contributorUser.getUserId() + "/revoke")
                .with(authentication(authenticationFor(adminUser)))).andReturn();
        User revokedUser = userRepository.findById(contributorUser.getUserId()).orElseThrow();

        Assertions.assertEquals(200, contributorsResult.getResponse().getStatus());
        Assertions.assertEquals(true, contributorsResult.getResponse().getContentAsString().contains("Contributor"));
        Assertions.assertEquals(200, revokeResult.getResponse().getStatus());
        Assertions.assertEquals(UserRole.USER, revokedUser.getRole());
        Assertions.assertEquals(ContributorRequestStatus.REVOKED, revokedUser.getRoleRequestStatus());
    }

    @Test
    @DisplayName("IT-ADM-04: regular user cannot access admin contributor endpoints")
    public void regularUserCannotAccessAdminContributorEndpoints() throws Exception {
        var result = mvc.perform(get("/api/admin/contributor-requests")
                .with(authentication(authenticationFor(regularUser)))).andReturn();

        Assertions.assertEquals(403, result.getResponse().getStatus());
    }
}
