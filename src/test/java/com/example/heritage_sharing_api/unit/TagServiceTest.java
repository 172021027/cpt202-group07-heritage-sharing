package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.entity.ResourceTag;
import com.example.heritage_sharing_api.entity.Tag;
import com.example.heritage_sharing_api.repository.ResourceTagRepository;
import com.example.heritage_sharing_api.repository.TagRepository;
import com.example.heritage_sharing_api.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ResourceTagRepository resourceTagRepository;

    @InjectMocks
    private TagService tagService;

    private Tag primaryTag;
    private Tag secondaryTag;

    @BeforeEach
    void setUp() {
        primaryTag = new Tag(1L, "Cultural Heritage");
        secondaryTag = new Tag(2L, "Old Building");
    }

    @Test
    void testGetAllTags() {
        List<Tag> expectedTags = Arrays.asList(primaryTag, secondaryTag);
        when(tagRepository.findAll()).thenReturn(expectedTags);

        List<Tag> actualTags = tagService.getAllTags();

        assertNotNull(actualTags);
        assertEquals(2, actualTags.size());
        assertEquals("Cultural Heritage", actualTags.get(0).getTagName());
    }

    @Test
    void testGetTagById_Success() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(primaryTag));

        Tag actualTag = tagService.getTagById(1L);

        assertNotNull(actualTag);
        assertEquals(1L, actualTag.getTagId());
        assertEquals("Cultural Heritage", actualTag.getTagName());
    }

    @Test
    void testGetTagById_NotFound() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());
        Tag actualTag = tagService.getTagById(99L);
        assertNull(actualTag);
    }

    @Test
    void testSaveTag() {
        Tag newTag = new Tag(null, "New Tag");
        Tag savedTag = new Tag(3L, "New Tag");
        when(tagRepository.save(newTag)).thenReturn(savedTag);

        Tag actual = tagService.saveTag(newTag);

        assertNotNull(actual);
        assertEquals(3L, actual.getTagId());
        assertEquals("New Tag", actual.getTagName());
    }

    @Test
    void testDeleteTag() {
        Long idToDelete = 1L;
        tagService.deleteTag(idToDelete);
        verify(tagRepository, times(1)).deleteById(idToDelete);
    }

    @Test
    void testMergeTags_Success() throws Exception {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(primaryTag));
        when(tagRepository.findById(2L)).thenReturn(Optional.of(secondaryTag));

        ResourceTag resourceTag = new ResourceTag();
        resourceTag.setResourceId(100L);
        resourceTag.setTagId(2L);
        when(resourceTagRepository.findByTagId(2L)).thenReturn(Arrays.asList(resourceTag));
        when(resourceTagRepository.existsByResourceIdAndTagId(100L, 1L)).thenReturn(false);

        tagService.mergeTags(2L, 1L);

        ArgumentCaptor<ResourceTag> savedResourceTag = ArgumentCaptor.forClass(ResourceTag.class);
        assertEquals(2L, resourceTag.getTagId());
        verify(resourceTagRepository, times(1)).delete(resourceTag);
        verify(resourceTagRepository, times(1)).save(savedResourceTag.capture());
        assertEquals(100L, savedResourceTag.getValue().getResourceId());
        assertEquals(1L, savedResourceTag.getValue().getTagId());
        verify(tagRepository, times(1)).deleteById(2L);
    }

    @Test
    void testMergeTags_DuplicatePrimaryTag() throws Exception {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(primaryTag));
        when(tagRepository.findById(2L)).thenReturn(Optional.of(secondaryTag));

        ResourceTag resourceTag = new ResourceTag();
        resourceTag.setResourceId(100L);
        resourceTag.setTagId(2L);
        when(resourceTagRepository.findByTagId(2L)).thenReturn(Arrays.asList(resourceTag));
        when(resourceTagRepository.existsByResourceIdAndTagId(100L, 1L)).thenReturn(true);

        tagService.mergeTags(2L, 1L);

        verify(resourceTagRepository, times(1)).delete(resourceTag);
        verify(tagRepository, times(1)).deleteById(2L);
    }

    @Test
    void testMergeTags_ThrowsExceptionWhenTagNotFound() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(primaryTag));
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            tagService.mergeTags(99L, 1L);
        });

        assertEquals("One or both tags not found", exception.getMessage());
    }

    @Test
    void testMergeTags_ThrowsExceptionWhenMergingSameTag() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(primaryTag));

        Exception exception = assertThrows(Exception.class, () -> {
            tagService.mergeTags(1L, 1L);
        });

        assertEquals("Cannot merge a tag with itself", exception.getMessage());
    }
}
