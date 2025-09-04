package com.example.Inventory.Management.Service;

import com.example.Inventory.Management.Entity.Category;
import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Exception.ProductNotFoundException;
import com.example.Inventory.Management.Repository.ProductRepository;
import com.example.Inventory.Management.Service.Impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic items")
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .price(1000.0)
                .quantity(10)
                .category(category)
                .build();
    }

    @Test
    void saveProduct_ShouldReturnSavedProduct() {
        // Given
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        Product savedProduct = productService.saveProduct(product);

        // Then
        assertNotNull(savedProduct);
        assertEquals("Laptop", savedProduct.getName());
        assertEquals(1000.0, savedProduct.getPrice());
        verify(productRepository).save(product);
    }

    @Test
    void getProductById_ShouldReturnProduct_WhenProductExists() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // When
        Product foundProduct = productService.getProductById(1L);

        // Then
        assertNotNull(foundProduct);
        assertEquals(1L, foundProduct.getId());
        assertEquals("Laptop", foundProduct.getName());
        verify(productRepository).findById(1L);
    }

    @Test
    void getProductById_ShouldThrowException_WhenProductNotFound() {
        // Given
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProductNotFoundException.class, () -> {
            productService.getProductById(999L);
        });
        verify(productRepository).findById(999L);
    }

    @Test
    void getAllProducts_ShouldReturnAllProducts() {
        // Given
        List<Product> products = Arrays.asList(product, Product.builder().id(2L).name("Mouse").build());
        when(productRepository.findAll()).thenReturn(products);

        // When
        List<Product> allProducts = productService.getAllProducts();

        // Then
        assertNotNull(allProducts);
        assertEquals(2, allProducts.size());
        verify(productRepository).findAll();
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProduct() {
        // Given
        Product existingProduct = Product.builder()
                .id(1L)
                .name("Old Laptop")
                .price(800.0)
                .quantity(5)
                .build();

        Product updatedProductData = Product.builder()
                .name("New Laptop")
                .price(1200.0)
                .quantity(15)
                .category(category)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // When
        Product result = productService.updateProduct(1L, updatedProductData);

        // Then
        assertNotNull(result);
        assertEquals("New Laptop", result.getName());
        assertEquals(1200.0, result.getPrice());
        assertEquals(15, result.getQuantity());
        verify(productRepository).findById(1L);
        verify(productRepository).save(existingProduct);
    }

    @Test
    void deleteProduct_ShouldDeleteProduct_WhenNoStockEntries() {
        // Given
        Product productToDelete = Product.builder()
                .id(1L)
                .name("Laptop")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(productToDelete));

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).findById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void searchProductsByName_ShouldReturnMatchingProducts() {
        // Given
        List<Product> products = Arrays.asList(product);
        when(productRepository.findByNameContainingIgnoreCase("Laptop")).thenReturn(products);

        // When
        List<Product> result = productService.searchProductsByName("Laptop");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getName());
        verify(productRepository).findByNameContainingIgnoreCase("Laptop");
    }

    @Test
    void getLowStockProducts_ShouldReturnProductsBelowThreshold() {
        // Given
        List<Product> lowStockProducts = Arrays.asList(
                Product.builder().id(1L).name("Low Stock Item").quantity(2).build()
        );
        when(productRepository.findByQuantityLessThan(5)).thenReturn(lowStockProducts);

        // When
        List<Product> result = productService.getLowStockProducts(5);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getQuantity());
        verify(productRepository).findByQuantityLessThan(5);
    }
}
