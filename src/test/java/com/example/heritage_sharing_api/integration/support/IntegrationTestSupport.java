package com.example.heritage_sharing_api.integration.support;

import com.example.heritage_sharing_api.entity.Category;
import com.example.heritage_sharing_api.entity.ContributorRequestStatus;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceAction;
import com.example.heritage_sharing_api.entity.ResourceActionType;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import com.example.heritage_sharing_api.repository.CategoryRepository;
import com.example.heritage_sharing_api.repository.ResourceActionRepository;
import com.example.heritage_sharing_api.repository.ResourceCommentRepository;
import com.example.heritage_sharing_api.repository.ResourceRepository;
import com.example.heritage_sharing_api.repository.ResourceTagRepository;
import com.example.heritage_sharing_api.repository.TagRepository;
import com.example.heritage_sharing_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    protected static final String TEST_PASSWORD = "Password123!";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected ResourceRepository resourceRepository;

    @Autowired
    protected TagRepository tagRepository;

    @Autowired
    protected ResourceTagRepository resourceTagRepository;

    @Autowired
    protected ResourceActionRepository resourceActionRepository;

    @Autowired
    protected ResourceCommentRepository resourceCommentRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected User contributorUser;
    protected User regularUser;
    protected User otherUser;
    protected User adminUser;
    protected Category category;

    @BeforeEach
    public void resetDatabase() {
        resourceCommentRepository.deleteAll();
        resourceTagRepository.deleteAll();
        resourceActionRepository.deleteAll();
        resourceRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        category = categoryRepository.save(new Category(null, "Intangible Heritage"));
        regularUser = userRepository.save(buildUser("RegularUser", "regular.integration@example.com", UserRole.USER, ContributorRequestStatus.NONE));
        otherUser = userRepository.save(buildUser("Visitor", "visitor.integration@example.com", UserRole.USER, ContributorRequestStatus.NONE));
        contributorUser = userRepository.save(buildUser("Contributor", "contributor.integration@example.com", UserRole.CONTRIBUTOR, ContributorRequestStatus.APPROVED));
        adminUser = userRepository.save(buildUser("Admin", "admin.integration@example.com", UserRole.ADMIN, ContributorRequestStatus.NONE));
    }

    protected User buildUser(String username, String email, UserRole role, ContributorRequestStatus requestStatus) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(role);
        user.setRoleRequestStatus(requestStatus);
        return user;
    }

    protected User savePendingContributorApplicant(String username, String email) {
        User user = buildUser(username, email, UserRole.USER, ContributorRequestStatus.PENDING);
        return userRepository.save(user);
    }

    protected Resource saveResource(String title, ResourceStatus status) {
        return resourceRepository.save(buildResource(contributorUser.getUserId(), title, status));
    }

    protected Resource buildResource(Long contributorId, String title, ResourceStatus status) {
        Resource resource = new Resource();
        resource.setContributorId(contributorId);
        resource.setTitle(title);
        resource.setDescription(title + " description");
        resource.setCategoryId(category.getCategoryId());
        resource.setLocation("Xi'an");
        resource.setPicturePath("uploads/image/" + title.toLowerCase().replace(" ", "-") + ".jpg");
        resource.setVideoPath("uploads/video/" + title.toLowerCase().replace(" ", "-") + ".mp4");
        resource.setCopyrightDeclaration("I own the rights");
        resource.setStatus(status);
        resource.setSubmittedAt(LocalDateTime.now());
        if (status == ResourceStatus.APPROVED) {
            resource.setApprovedAt(LocalDateTime.now());
        }
        return resource;
    }

    protected ResourceAction saveAction(Long resourceId, ResourceActionType actionType, Long actionByUserId, String feedbackText) {
        ResourceAction action = new ResourceAction();
        action.setResourceId(resourceId);
        action.setActionType(actionType);
        action.setActionByUserId(actionByUserId);
        action.setActionAt(LocalDateTime.now());
        action.setFeedbackText(feedbackText);
        return resourceActionRepository.save(action);
    }

    protected UsernamePasswordAuthenticationToken authenticationFor(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getUserId(),
                null,
                List.of(new SimpleGrantedAuthority(user.getRole().getAuthority()))
        );
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
