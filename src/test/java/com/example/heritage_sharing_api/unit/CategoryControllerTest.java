package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.controller.CategoryController;
import com.example.heritage_sharing_api.entity.Category;
import com.example.heritage_sharing_api.service.CategoryService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for CategoryController")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
        objectMapper = new ObjectMapper();
        testCategory = new Category(1L, "Historical Sites");
    }

    @Test
    @DisplayName("UT-CAT-01: getAllCategories returns 200 and list of categories")
    void getAllCategoriesReturnsSuccess() throws Exception {
        List<Category> categories = Arrays.asList(testCategory, new Category(2L, "Artifacts"));

        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].categoryName").value("Historical Sites"));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("UT-CAT-02: getCategoryById returns 200 when category exists")
    void getCategoryByIdReturnsSuccessWhenExists() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(testCategory);

        mockMvc.perform(get("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Historical Sites"));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    @Test
    @DisplayName("UT-CAT-03: saveCategory returns 200 on successful creation")
    void saveCategoryReturnsSuccess() throws Exception {
        Category newCategory = new Category(null, "Monuments");
        Category savedCategory = new Category(3L, "Monuments");

        when(categoryService.saveCategory(any(Category.class))).thenReturn(savedCategory);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(3))
                .andExpect(jsonPath("$.categoryName").value("Monuments"));

        verify(categoryService, times(1)).saveCategory(any(Category.class));
    }

    @Test
    @DisplayName("UT-CAT-04: deleteCategory returns 200 on successful deletion")
    void deleteCategoryReturnsSuccess() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(testCategory);
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getCategoryById(1L);
        verify(categoryService, times(1)).deleteCategory(1L);
    }

    @Test
    @DisplayName("UT-CAT-05: getCategoryById returns 404 when category not found")
    void getCategoryByIdReturnsNotFound() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/categories/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).getCategoryById(99L);
    }

    @Test
    @DisplayName("UT-CAT-06: getAllCategories returns empty list when no categories")
    void getAllCategoriesReturnsEmptyList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));

        verify(categoryService, times(1)).getAllCategories();
    }
}