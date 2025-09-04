package com.example.Inventory.Management.Service;

import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Exception.InsufficientStockException;
import com.example.Inventory.Management.Exception.ProductNotFoundException;
import com.example.Inventory.Management.Exception.StockEntryNotFoundException;
import com.example.Inventory.Management.Repository.ProductRepository;
import com.example.Inventory.Management.Repository.StockEntryRepository;
import com.example.Inventory.Management.Service.Impl.StockEntryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockEntryServiceImplTest {

    @Mock
    private StockEntryRepository stockEntryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockEntryServiceImpl stockEntryService;

    private Product product;
    private StockEntry stockEntry;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .quantity(20)
                .price(100.0)
                .build();

        stockEntry = StockEntry.builder()
                .id(1L)
                .product(product)
                .quantity(5)
                .type("PURCHASE")
                .date(LocalDateTime.now())
                .build();
    }

    @Test
    void addStockEntry_Purchase_ShouldIncreaseProductQuantity() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(stockEntryRepository.save(any(StockEntry.class))).thenReturn(stockEntry);

        // When
        StockEntry result = stockEntryService.addStockEntry(stockEntry);

        // Then
        assertNotNull(result);
        assertEquals("PURCHASE", result.getType());
        assertEquals(25, product.getQuantity()); 
        verify(productRepository).save(product);
        verify(stockEntryRepository).save(stockEntry);
    }

    @Test
    void addStockEntry_Sale_ShouldDecreaseProductQuantity() {
        // Given
        StockEntry saleEntry = StockEntry.builder()
                .product(product)
                .quantity(3)
                .type("SALE")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(stockEntryRepository.save(any(StockEntry.class))).thenReturn(saleEntry);

        // When
        StockEntry result = stockEntryService.addStockEntry(saleEntry);

        // Then
        assertNotNull(result);
        assertEquals("SALE", result.getType());
        assertEquals(17, product.getQuantity()); 
        verify(productRepository).save(product);
    }

    @Test
    void addStockEntry_Sale_ShouldThrowException_WhenInsufficientStock() {
        // Given
        Product lowStockProduct = Product.builder()
                .id(1L)
                .name("Low Stock Product")
                .quantity(2)
                .build();

        StockEntry largeSaleEntry = StockEntry.builder()
                .product(lowStockProduct)
                .quantity(5)
                .type("SALE")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(lowStockProduct));

        // When & Then
        assertThrows(InsufficientStockException.class, () -> {
            stockEntryService.addStockEntry(largeSaleEntry);
        });

        verify(productRepository, never()).save(any(Product.class));
        verify(stockEntryRepository, never()).save(any(StockEntry.class));
    }

    @Test
    void addStockEntry_ShouldThrowException_WhenProductNotFound() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        StockEntry entryWithInvalidProduct = StockEntry.builder()
                .product(Product.builder().id(999L).build())
                .quantity(5)
                .type("PURCHASE")
                .build();

        // When & Then
        assertThrows(ProductNotFoundException.class, () -> {
            stockEntryService.addStockEntry(entryWithInvalidProduct);
        });
    }

    @Test
    void addStockEntry_ShouldThrowException_WhenInvalidType() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        StockEntry invalidTypeEntry = StockEntry.builder()
                .product(product)
                .quantity(5)
                .type("INVALID")
                .build();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            stockEntryService.addStockEntry(invalidTypeEntry);
        });

        assertTrue(exception.getMessage().contains("Invalid stock entry type"));
    }

    @Test
    void getStockEntryById_ShouldReturnStockEntry_WhenExists() {
        // Given
        when(stockEntryRepository.findById(1L)).thenReturn(Optional.of(stockEntry));

        // When
        StockEntry result = stockEntryService.getStockEntryById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(5, result.getQuantity());
    }

    @Test
    void getStockEntryById_ShouldThrowException_WhenNotFound() {
        // Given
        when(stockEntryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(StockEntryNotFoundException.class, () -> {
            stockEntryService.getStockEntryById(999L);
        });
    }

    @Test
    void deleteStockEntry_Sale_ShouldRestoreProductQuantity() {
        // Given
        StockEntry saleEntry = StockEntry.builder()
                .id(1L)
                .product(product)
                .quantity(5)
                .type("SALE")
                .build();

        when(stockEntryRepository.findById(1L)).thenReturn(Optional.of(saleEntry));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        stockEntryService.deleteStockEntry(1L);

        // Then
        assertEquals(25, product.getQuantity()); // 20 + 5 (restored)
        verify(productRepository).save(product);
        verify(stockEntryRepository).deleteById(1L);
    }
}
