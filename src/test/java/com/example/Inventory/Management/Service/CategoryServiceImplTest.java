package com.example.Inventory.Management.Service;

import com.example.Inventory.Management.Entity.Category;
import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Exception.CategoryNotFoundException;
import com.example.Inventory.Management.Repository.CategoryRepository;
import com.example.Inventory.Management.Service.Impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic products")
                .products(new HashSet<>())
                .build();
    }

    @Test
    void saveCategory_ShouldReturnSavedCategory() {
        // Given
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        Category result = categoryService.saveCategory(category);

        // Then
        assertNotNull(result);
        assertEquals("Electronics", result.getName());
        verify(categoryRepository).save(category);
    }

    @Test
    void getCategoryById_ShouldReturnCategory_WhenExists() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // When
        Category result = categoryService.getCategoryById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Electronics", result.getName());
    }

    @Test
    void getCategoryById_ShouldThrowException_WhenNotFound() {
        // Given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CategoryNotFoundException.class, () -> {
            categoryService.getCategoryById(999L);
        });
    }

    @Test
    void deleteCategory_ShouldDelete_WhenNoAssociatedProducts() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // When
        categoryService.deleteCategory(1L);

        // Then
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_ShouldThrowException_WhenHasAssociatedProducts() {
        // Given
        Set<Product> products = new HashSet<>();
        products.add(Product.builder().id(1L).name("Laptop").build());
        category.setProducts(products);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categoryService.deleteCategory(1L);
        });

        assertTrue(exception.getMessage().contains("Cannot delete category that has associated products"));
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void updateCategory_ShouldReturnUpdatedCategory() {
        // Given
        Category existingCategory = Category.builder()
                .id(1L)
                .name("Old Name")
                .description("Old Description")
                .build();

        Category updateData = Category.builder()
                .name("New Electronics")
                .description("New electronic products")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);

        // When
        Category result = categoryService.updateCategory(1L, updateData);

        // Then
        assertNotNull(result);
        assertEquals("New Electronics", result.getName());
        assertEquals("New electronic products", result.getDescription());
        verify(categoryRepository).save(existingCategory);
    }
}
