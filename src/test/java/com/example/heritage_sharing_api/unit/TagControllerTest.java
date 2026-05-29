package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.controller.TagController;
import com.example.heritage_sharing_api.entity.Tag;
import com.example.heritage_sharing_api.service.TagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for TagController")
class TagControllerTest {

    @Mock
    private TagService tagService;

    @InjectMocks
    private TagController tagController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tagController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("UT-TAG-01: saveTag returns 200 and saved tag")
    void saveTagReturnsSuccess() throws Exception {
        Tag inputTag = new Tag(null, "New Tag");
        Tag savedTag = new Tag(1L, "New Tag");

        when(tagService.saveTag(any(Tag.class))).thenReturn(savedTag);

        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputTag)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").value(1))
                .andExpect(jsonPath("$.tagName").value("New Tag"));

        verify(tagService, times(1)).saveTag(any(Tag.class));
    }

    @Test
    @DisplayName("UT-TAG-02: mergeTags returns 200 when merge is successful")
    void mergeTagsReturnsSuccess() throws Exception {
        doNothing().when(tagService).mergeTags(2L, 1L);

        mockMvc.perform(post("/api/tags/merge")
                        .param("secondaryTagId", "2")
                        .param("primaryTagId", "1"))
                .andExpect(status().isOk());

        verify(tagService, times(1)).mergeTags(2L, 1L);
    }

    @Test
    @DisplayName("UT-TAG-03: mergeTags returns Bad Request when merging same tag")
    void mergeTagsReturnsBadRequestWhenMergingSameTag() throws Exception {
        doThrow(new Exception("Cannot merge a tag with itself")).when(tagService).mergeTags(1L, 1L);

        mockMvc.perform(post("/api/tags/merge")
                        .param("secondaryTagId", "1")
                        .param("primaryTagId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot merge a tag with itself"));
    }

    @Test
    @DisplayName("UT-TAG-04: mergeTags returns Bad Request when tag not found")
    void mergeTagsReturnsBadRequestWhenTagNotFound() throws Exception {
        doThrow(new Exception("One or both tags not found")).when(tagService).mergeTags(99L, 1L);

        mockMvc.perform(post("/api/tags/merge")
                        .param("secondaryTagId", "99")
                        .param("primaryTagId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("One or both tags not found"));
    }

    @Test
    @DisplayName("UT-TAG-05: getAllTags returns 200 and list of tags")
    void getAllTagsReturnsSuccess() throws Exception {
        List<Tag> tags = Arrays.asList(new Tag(1L, "Tag1"), new Tag(2L, "Tag2"));
        when(tagService.getAllTags()).thenReturn(tags);

        mockMvc.perform(get("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));

        verify(tagService, times(1)).getAllTags();
    }

    @Test
    @DisplayName("UT-TAG-06: getTagById returns 200 when tag exists")
    void getTagByIdReturnsSuccessWhenExists() throws Exception {
        Tag tag = new Tag(1L, "Test Tag");
        when(tagService.getTagById(1L)).thenReturn(tag);

        mockMvc.perform(get("/api/tags/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").value(1))
                .andExpect(jsonPath("$.tagName").value("Test Tag"));

        verify(tagService, times(1)).getTagById(1L);
    }

    @Test
    @DisplayName("UT-TAG-07: deleteTag returns 200 on successful deletion")
    void deleteTagReturnsSuccess() throws Exception {
        Tag tag = new Tag(1L, "Test Tag");
        when(tagService.getTagById(1L)).thenReturn(tag);
        doNothing().when(tagService).deleteTag(1L);

        mockMvc.perform(delete("/api/tags/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(tagService, times(1)).getTagById(1L);
        verify(tagService, times(1)).deleteTag(1L);
    }
}