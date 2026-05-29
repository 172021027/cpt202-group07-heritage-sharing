package com.example.heritage_sharing_api.unit;
import static org.mockito.Mockito.lenient;
import com.example.heritage_sharing_api.dto.PublicResourceDto;
import com.example.heritage_sharing_api.entity.Category;
import com.example.heritage_sharing_api.entity.Resource;
import com.example.heritage_sharing_api.entity.ResourceStatus;
import com.example.heritage_sharing_api.entity.ResourceTag;
import com.example.heritage_sharing_api.entity.Tag;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.repository.CategoryRepository;
import com.example.heritage_sharing_api.repository.ResourceRepository;
import com.example.heritage_sharing_api.repository.ResourceTagRepository;
import com.example.heritage_sharing_api.repository.TagRepository;
import com.example.heritage_sharing_api.repository.UserRepository;
import com.example.heritage_sharing_api.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for ResourceService search and filter functionality")
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ResourceTagRepository resourceTagRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Resource resource1;
    private Resource resource2;
    private Category category1;
    private Category category2;
    private User user1;
    private User user2;
    private Tag tag1;
    private Tag tag2;
    private ResourceTag resourceTag1;
    private ResourceTag resourceTag2;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        category1 = new Category();
        category1.setCategoryId(1L);
        category1.setCategoryName("Architecture");

        category2 = new Category();
        category2.setCategoryId(2L);
        category2.setCategoryName("Art");

        user1 = new User();
        user1.setUserId(1L);
        user1.setUsername("Contributor1");

        user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("Contributor2");

        tag1 = new Tag();
        tag1.setTagId(1L);
        tag1.setTagName("ancient");

        tag2 = new Tag();
        tag2.setTagId(2L);
        tag2.setTagName("painting");

        resource1 = new Resource();
        resource1.setResourceId(1L);
        resource1.setContributorId(1L);
        resource1.setTitle("Ancient Temple");
        resource1.setDescription("A beautiful ancient temple in Beijing");
        resource1.setCategoryId(1L);
        resource1.setLocation("Beijing");
        resource1.setPicturePath("uploads/image/temple.jpg");
        resource1.setVideoPath("uploads/video/temple.mp4");
        resource1.setCopyrightDeclaration("Copyright 2024");
        resource1.setStatus(ResourceStatus.APPROVED);
        resource1.setSubmittedAt(now.minusDays(10));
        resource1.setApprovedAt(now.minusDays(9));

        resource2 = new Resource();
        resource2.setResourceId(2L);
        resource2.setContributorId(2L);
        resource2.setTitle("Traditional Painting");
        resource2.setDescription("A classic Chinese painting from Shanghai");
        resource2.setCategoryId(2L);
        resource2.setLocation("Shanghai");
        resource2.setPicturePath("uploads/image/painting.jpg");
        resource2.setVideoPath("uploads/video/painting.mp4");
        resource2.setCopyrightDeclaration("Copyright 2024");
        resource2.setStatus(ResourceStatus.APPROVED);
        resource2.setSubmittedAt(now.minusDays(5));
        resource2.setApprovedAt(now.minusDays(4));

        resourceTag1 = new ResourceTag();
        resourceTag1.setResourceId(1L);
        resourceTag1.setTagId(1L);

        resourceTag2 = new ResourceTag();
        resourceTag2.setResourceId(2L);
        resourceTag2.setTagId(2L);
    }

    @Test
    @DisplayName("UT-RS-01: searchResources returns resources matching keyword in title")
    void searchResourcesReturnsResourcesMatchingKeywordInTitle() {
        lenient().when(resourceRepository.findByStatusInAndTitleContainingIgnoreCaseOrderBySubmittedAtDesc(
                any(), any()))
                .thenReturn(Arrays.asList(resource1));
        lenient().when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1));
        lenient().when(userRepository.findAll()).thenReturn(Arrays.asList(user1));
        lenient().when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag1));
        lenient().when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag1));

        List<PublicResourceDto> result = resourceService.searchResources("temple");

        assertEquals(1, result.size());
        assertEquals("Ancient Temple", result.get(0).title());
    }

    @Test
    @DisplayName("UT-RS-02: searchResources returns resources matching keyword in description")
    void searchResourcesReturnsResourcesMatchingKeywordInDescription() {
        lenient().when(resourceRepository.findByStatusInAndTitleContainingIgnoreCaseOrderBySubmittedAtDesc(
                any(), any()))
                .thenReturn(Arrays.asList(resource2));
        lenient().when(categoryRepository.findAll()).thenReturn(Arrays.asList(category2));
        lenient().when(userRepository.findAll()).thenReturn(Arrays.asList(user2));
        lenient().when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag2));
        lenient().when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag2));

        List<PublicResourceDto> result = resourceService.searchResources("Shanghai");

        assertEquals(1, result.size());
        assertEquals("Traditional Painting", result.get(0).title());
    }

    @Test
    @DisplayName("UT-RS-03: searchResources returns empty list when no matches")
    void searchResourcesReturnsEmptyListWhenNoMatches() {
        lenient().when(resourceRepository.findByStatusInAndTitleContainingIgnoreCaseOrderBySubmittedAtDesc(
                any(), any()))
                .thenReturn(Collections.emptyList());

        List<PublicResourceDto> result = resourceService.searchResources("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("UT-RS-04: searchResources is case insensitive")
    void searchResourcesIsCaseInsensitive() {
        lenient().when(resourceRepository.findByStatusInAndTitleContainingIgnoreCaseOrderBySubmittedAtDesc(
                any(), any()))
                .thenReturn(Arrays.asList(resource1));
        lenient().when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1));
        lenient().when(userRepository.findAll()).thenReturn(Arrays.asList(user1));
        lenient().when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag1));
        lenient().when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag1));

        List<PublicResourceDto> result = resourceService.searchResources("TEMPLE");

        assertEquals(1, result.size());
        assertEquals("Ancient Temple", result.get(0).title());
    }

    @Test
    @DisplayName("UT-RS-05: filterResources returns resources matching category")
    void filterResourcesReturnsResourcesMatchingCategory() {
        when(resourceRepository.findByStatusOrderBySubmittedAtDesc(ResourceStatus.APPROVED))
                .thenReturn(Arrays.asList(resource1));
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag1));
        when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag1));

        List<PublicResourceDto> result = resourceService.filterResources(1L, null, null);

        assertEquals(1, result.size());
        assertEquals("Architecture", result.get(0).category());
    }

    @Test
    @DisplayName("UT-RS-06: filterResources returns resources matching tag")
    void filterResourcesReturnsResourcesMatchingTag() {
        when(resourceRepository.findByStatusOrderBySubmittedAtDesc(ResourceStatus.APPROVED))
                .thenReturn(Arrays.asList(resource1));
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        when(tagRepository.findByTagNameIgnoreCase("ancient")).thenReturn(Optional.of(tag1));
        when(resourceTagRepository.findByTagId(1L)).thenReturn(Collections.singletonList(resourceTag1));
        when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag1));
        when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag1));

        List<PublicResourceDto> result = resourceService.filterResources(null, "ancient", null);

        assertEquals(1, result.size());
        assertEquals("Ancient Temple", result.get(0).title());
    }

    @Test
    @DisplayName("UT-RS-07: filterResources returns resources matching location")
    void filterResourcesReturnsResourcesMatchingLocation() {
        when(resourceRepository.findByStatusOrderBySubmittedAtDesc(ResourceStatus.APPROVED))
                .thenReturn(Arrays.asList(resource1));
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag1));
        when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag1));

        List<PublicResourceDto> result = resourceService.filterResources(null, null, "Beijing");

        assertEquals(1, result.size());
        assertEquals("Beijing", result.get(0).location());
    }

    @Test
    @DisplayName("UT-RS-08: filterResources returns resources matching multiple filters")
    void filterResourcesReturnsResourcesMatchingMultipleFilters() {
        when(resourceRepository.findByStatusOrderBySubmittedAtDesc(ResourceStatus.APPROVED))
                .thenReturn(Arrays.asList(resource1));
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        when(tagRepository.findByTagNameIgnoreCase("ancient")).thenReturn(Optional.of(tag1));
        when(resourceTagRepository.findByTagId(1L)).thenReturn(Collections.singletonList(resourceTag1));
        when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag1));
        when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag1));

        List<PublicResourceDto> result = resourceService.filterResources(1L, "ancient", "Beijing");

        assertEquals(1, result.size());
        assertEquals("Ancient Temple", result.get(0).title());
    }

    @Test
    @DisplayName("UT-RS-09: filterResources excludes pending review resources")
    void filterResourcesExcludesPendingReviewResources() {
        when(resourceRepository.findByStatusOrderBySubmittedAtDesc(ResourceStatus.APPROVED))
                .thenReturn(Arrays.asList(resource2));
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag2));
        when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag2));

        List<PublicResourceDto> result = resourceService.filterResources(2L, null, null);

        assertEquals(1, result.size());
        assertEquals("Traditional Painting", result.get(0).title());
    }

    @Test
    @DisplayName("UT-RS-10: searchAndFilterResources combines keyword search with category filter")
    void searchAndFilterResourcesCombinesKeywordSearchWithCategoryFilter() {
        when(resourceRepository.findByStatusOrderBySubmittedAtDesc(ResourceStatus.APPROVED))
                .thenReturn(Arrays.asList(resource1));
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag1));
        when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag1));

        List<PublicResourceDto> result = resourceService.searchAndFilterResources("temple", 1L, null, null);

        assertEquals(1, result.size());
        assertEquals("Ancient Temple", result.get(0).title());
    }

    @Test
    @DisplayName("UT-RS-11: searchAndFilterResources returns empty when category mismatch")
    void searchAndFilterResourcesReturnsEmptyWhenCategoryMismatch() {
        when(resourceRepository.findByStatusOrderBySubmittedAtDesc(ResourceStatus.APPROVED))
                .thenReturn(Arrays.asList(resource2));

        List<PublicResourceDto> result = resourceService.searchAndFilterResources("temple", 2L, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("UT-RS-12: searchAndFilterResources handles all criteria together")
    void searchAndFilterResourcesHandlesAllCriteriaTogether() {
        when(resourceRepository.findByStatusOrderBySubmittedAtDesc(ResourceStatus.APPROVED))
                .thenReturn(Arrays.asList(resource1));
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        when(tagRepository.findByTagNameIgnoreCase("ancient")).thenReturn(Optional.of(tag1));
        when(resourceTagRepository.findByTagId(1L)).thenReturn(Collections.singletonList(resourceTag1));
        when(resourceTagRepository.findByResourceIdIn(any())).thenReturn(Collections.singletonList(resourceTag1));
        when(tagRepository.findAllById(any())).thenReturn(Collections.singletonList(tag1));

        List<PublicResourceDto> result = resourceService.searchAndFilterResources("ancient", 1L, "ancient", "Beijing");

        assertEquals(1, result.size());
        assertEquals("Ancient Temple", result.get(0).title());
    }
}
