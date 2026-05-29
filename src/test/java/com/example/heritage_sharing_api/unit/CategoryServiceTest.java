package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.entity.Category;
import com.example.heritage_sharing_api.repository.CategoryRepository;
import com.example.heritage_sharing_api.service.CategoryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void testSaveCategory_NormalInput() {
        Category input = new Category(null, "Historical Sites");
        Category expectedResult = new Category(1L, "Historical Sites");

        Mockito.when(categoryRepository.save(input)).thenReturn(expectedResult);

        Category actualResult = categoryService.saveCategory(input);

        Assertions.assertNotNull(actualResult);
        Assertions.assertEquals(expectedResult.getCategoryId(), actualResult.getCategoryId());
        Assertions.assertEquals(expectedResult.getCategoryName(), actualResult.getCategoryName());
    }

    @Test
    void testSaveCategory_EmptyInput() {
        Category input = new Category(null, "");

        Mockito.when(categoryRepository.save(input)).thenThrow(new IllegalArgumentException("Name cannot be empty"));

        IllegalArgumentException actualException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.saveCategory(input)
        );

        Assertions.assertEquals("Name cannot be empty", actualException.getMessage());
    }

    @Test
    void testSaveCategory_BoundaryInput() {
        String boundaryName = "A".repeat(100);
        Category input = new Category(null, boundaryName);
        Category expectedResult = new Category(2L, boundaryName);

        Mockito.when(categoryRepository.save(input)).thenReturn(expectedResult);

        Category actualResult = categoryService.saveCategory(input);

        Assertions.assertEquals(expectedResult.getCategoryName(), actualResult.getCategoryName());
    }

    @Test
    void testSaveCategory_AbnormalInput() {
        Category input = null;

        Mockito.when(categoryRepository.save(null)).thenThrow(new IllegalArgumentException("Category cannot be null"));

        IllegalArgumentException actualException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.saveCategory(input)
        );

        Assertions.assertEquals("Category cannot be null", actualException.getMessage());
    }

    @Test
    void testGetCategoryById_NormalInput() {
        Long input = 1L;
        Category expectedResult = new Category(1L, "Historical Sites");

        Mockito.when(categoryRepository.findById(input)).thenReturn(Optional.of(expectedResult));

        Category actualResult = categoryService.getCategoryById(input);

        Assertions.assertNotNull(actualResult);
        Assertions.assertEquals(expectedResult.getCategoryName(), actualResult.getCategoryName());
    }

    @Test
    void testGetCategoryById_InvalidStateNotFound() {
        Long input = 99L;

        Mockito.when(categoryRepository.findById(input)).thenReturn(Optional.empty());

        Category actualResult = categoryService.getCategoryById(input);

        Assertions.assertNull(actualResult);
    }

    @Test
    void testGetAllCategories_NormalInput() {
        List<Category> expectedResult = Arrays.asList(
                new Category(1L, "Historical Sites"),
                new Category(2L, "Artifacts")
        );

        Mockito.when(categoryRepository.findAll()).thenReturn(expectedResult);

        List<Category> actualResult = categoryService.getAllCategories();

        Assertions.assertNotNull(actualResult);
        Assertions.assertEquals(expectedResult.size(), actualResult.size());
    }

    @Test
    void testGetAllCategories_EmptyState() {
        List<Category> expectedResult = Collections.emptyList();

        Mockito.when(categoryRepository.findAll()).thenReturn(expectedResult);

        List<Category> actualResult = categoryService.getAllCategories();

        Assertions.assertNotNull(actualResult);
        Assertions.assertEquals(0, actualResult.size());
    }

    @Test
    void testDeleteCategory_NormalInput() {
        Long input = 1L;

        categoryService.deleteCategory(input);

        Mockito.verify(categoryRepository, Mockito.times(1)).deleteById(input);
    }
}