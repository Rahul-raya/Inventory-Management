package com.example.Inventory.Management.Service;

import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Repository.ProductRepository;
import com.example.Inventory.Management.Repository.StockEntryRepository;
import com.example.Inventory.Management.Service.ML.InventoryPrediction;
import com.example.Inventory.Management.Service.ML.LinearRegressionPredictionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinearRegressionPredictionServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockEntryRepository stockEntryRepository;

    @InjectMocks
    private LinearRegressionPredictionService predictionService;

    private Product product;
    private List<StockEntry> stockHistory;
    private List<StockEntry> largeStockHistory;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .quantity(100)
                .price(50.0)
                .build();

        // Create mock stock history with sales data 
        stockHistory = Arrays.asList(
                createStockEntry(1L, LocalDateTime.now().minusDays(10), "SALE", 5),
                createStockEntry(2L, LocalDateTime.now().minusDays(9), "SALE", 3),
                createStockEntry(3L, LocalDateTime.now().minusDays(8), "SALE", 7),
                createStockEntry(4L, LocalDateTime.now().minusDays(7), "SALE", 4),
                createStockEntry(5L, LocalDateTime.now().minusDays(6), "SALE", 6),
                createStockEntry(6L, LocalDateTime.now().minusDays(5), "PURCHASE", 50),
                createStockEntry(7L, LocalDateTime.now().minusDays(4), "SALE", 8),
                createStockEntry(8L, LocalDateTime.now().minusDays(3), "SALE", 2),
                createStockEntry(9L, LocalDateTime.now().minusDays(2), "SALE", 9),
                createStockEntry(10L, LocalDateTime.now().minusDays(1), "SALE", 5)
        );

        // Create larger stock history for multiple regression 
        largeStockHistory = Arrays.asList(
                createStockEntry(1L, LocalDateTime.now().minusDays(20), "SALE", 5),
                createStockEntry(2L, LocalDateTime.now().minusDays(19), "SALE", 3),
                createStockEntry(3L, LocalDateTime.now().minusDays(18), "SALE", 7),
                createStockEntry(4L, LocalDateTime.now().minusDays(17), "SALE", 4),
                createStockEntry(5L, LocalDateTime.now().minusDays(16), "SALE", 6),
                createStockEntry(6L, LocalDateTime.now().minusDays(15), "SALE", 8),
                createStockEntry(7L, LocalDateTime.now().minusDays(14), "SALE", 2),
                createStockEntry(8L, LocalDateTime.now().minusDays(13), "SALE", 9),
                createStockEntry(9L, LocalDateTime.now().minusDays(12), "SALE", 5),
                createStockEntry(10L, LocalDateTime.now().minusDays(11), "SALE", 4),
                createStockEntry(11L, LocalDateTime.now().minusDays(10), "SALE", 7),
                createStockEntry(12L, LocalDateTime.now().minusDays(9), "SALE", 6),
                createStockEntry(13L, LocalDateTime.now().minusDays(8), "SALE", 3),
                createStockEntry(14L, LocalDateTime.now().minusDays(7), "SALE", 8),
                createStockEntry(15L, LocalDateTime.now().minusDays(6), "SALE", 5)
        );
    }

    private StockEntry createStockEntry(Long id, LocalDateTime date, String type, int quantity) {
        return StockEntry.builder()
                .id(id)
                .product(product)
                .date(date)
                .type(type)
                .quantity(quantity)
                .build();
    }

    @Test
    void predictInventoryWithLinearRegression_ShouldReturnPrediction_WhenSufficientData() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockEntryRepository.findByProduct_Id(1L)).thenReturn(stockHistory);

        // When
        InventoryPrediction prediction = predictionService.predictInventoryWithLinearRegression(1L, 30);

        // Then
        assertNotNull(prediction);
        assertEquals(1L, prediction.getProductId());
        assertEquals("Test Product", prediction.getProductName());
        assertEquals(100, prediction.getCurrentStock());
        assertEquals(30, prediction.getDaysAhead());
        assertEquals("Linear Regression", prediction.getPredictionMethod());
        assertNotNull(prediction.getPredictedDemand());
        assertNotNull(prediction.getSafetyStock());
        assertNotNull(prediction.getReorderPoint());
        assertTrue(prediction.getConfidence() >= 0.0);
        assertTrue(prediction.getConfidence() <= 1.0);
        
        // Verify predicted demand is positive
        assertTrue(prediction.getPredictedDemand() >= 0, "Predicted demand should be non-negative");
        
        // Verify safety stock is calculated
        assertTrue(prediction.getSafetyStock() >= 0, "Safety stock should be non-negative");
    }

    @Test
    void predictInventoryWithLinearRegression_ShouldReturnSimplePrediction_WhenInsufficientData() {
        // Given
        List<StockEntry> limitedHistory = Arrays.asList(
                createStockEntry(1L, LocalDateTime.now().minusDays(1), "SALE", 2)
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockEntryRepository.findByProduct_Id(1L)).thenReturn(limitedHistory);

        // When
        InventoryPrediction prediction = predictionService.predictInventoryWithLinearRegression(1L, 30);

        // Then
        assertNotNull(prediction);
        assertEquals("Simple Average", prediction.getPredictionMethod());
        assertEquals(0.5, prediction.getConfidence());
        assertNotNull(prediction.getPredictedDemand());
        assertTrue(prediction.getPredictedDemand() > 0, "Simple prediction should have positive demand");
    }

    @Test
    void predictInventoryWithLinearRegression_ShouldThrowException_WhenProductNotFound() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            predictionService.predictInventoryWithLinearRegression(999L, 30);
        });
        
        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    void predictWithMultipleFeatures_ShouldReturnAdvancedPrediction_WhenSufficientData() {
        // Given 
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockEntryRepository.findByProduct_Id(1L)).thenReturn(largeStockHistory);

        // When
        InventoryPrediction prediction = predictionService.predictWithMultipleFeatures(1L, 30);

        // Then
        assertNotNull(prediction);
        assertEquals(1L, prediction.getProductId());
        assertEquals("Test Product", prediction.getProductName());
        assertEquals(100, prediction.getCurrentStock());
        assertEquals(30, prediction.getDaysAhead());
        assertEquals("Multiple Linear Regression", prediction.getPredictionMethod());
        
        assertNotNull(prediction.getPredictedDemand());
        assertTrue(prediction.getPredictedDemand() >= 0, "Predicted demand should be non-negative");
        
        assertNotNull(prediction.getSafetyStock());
        assertTrue(prediction.getSafetyStock() >= 0, "Safety stock should be non-negative");
        
        assertNotNull(prediction.getReorderPoint());
        assertTrue(prediction.getReorderPoint() >= 0, "Reorder point should be non-negative");
        
        // Confidence should be a valid value
        assertTrue(prediction.getConfidence() >= 0.0 && prediction.getConfidence() <= 1.0);
    }

    @Test
    void predictWithMultipleFeatures_ShouldFallBackToLinearRegression_WhenInsufficientData() {
        // Given 
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockEntryRepository.findByProduct_Id(1L)).thenReturn(stockHistory); // Only 10 entries

        // When
        InventoryPrediction prediction = predictionService.predictWithMultipleFeatures(1L, 30);

        // Then
        assertNotNull(prediction);
        // Should fall back to linear regression since we have exactly 10 entries (not > 10)
        assertEquals("Linear Regression", prediction.getPredictionMethod());
        assertNotNull(prediction.getPredictedDemand());
    }

    @Test
    void inventoryPrediction_ShouldCalculateRestockCorrectly() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockEntryRepository.findByProduct_Id(1L)).thenReturn(stockHistory);

        // When
        InventoryPrediction prediction = predictionService.predictInventoryWithLinearRegression(1L, 30);

        // Then
        assertNotNull(prediction.isRestockNeeded());
        assertNotNull(prediction.getRecommendedOrderQuantity());
        assertNotNull(prediction.getRiskLevel());
        assertNotNull(prediction.getStockoutDays());

        // Risk level should be valid
        String riskLevel = prediction.getRiskLevel();
        assertTrue(riskLevel.equals("LOW") || riskLevel.equals("MEDIUM") || riskLevel.equals("HIGH"));
        
        assertTrue(prediction.getRecommendedOrderQuantity() >= 0);
        
        // Stockout days should be positive
        assertTrue(prediction.getStockoutDays() > 0);
    }

    @Test
    void inventoryPrediction_ShouldIndicateRestockNeeded_WhenStockBelowReorderPoint() {
        // Given
        Product lowStockProduct = Product.builder()
                .id(2L)
                .name("Low Stock Product")
                .quantity(5)
                .price(50.0)
                .build();

        when(productRepository.findById(2L)).thenReturn(Optional.of(lowStockProduct));
        when(stockEntryRepository.findByProduct_Id(2L)).thenReturn(stockHistory);

        // When
        InventoryPrediction prediction = predictionService.predictInventoryWithLinearRegression(2L, 30);

        // Then
        assertNotNull(prediction);
        assertEquals(5, prediction.getCurrentStock());
        
        if (prediction.getCurrentStock() <= prediction.getReorderPoint()) {
            assertTrue(prediction.isRestockNeeded(), "Should need restock when stock is below reorder point");
            assertTrue(prediction.getRecommendedOrderQuantity() > 0, "Should recommend positive order quantity");
        }
    }

    @Test
    void inventoryPrediction_ShouldCalculateRiskLevelCorrectly() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockEntryRepository.findByProduct_Id(1L)).thenReturn(stockHistory);

        // When
        InventoryPrediction prediction = predictionService.predictInventoryWithLinearRegression(1L, 30);

        // Then
        String riskLevel = prediction.getRiskLevel();
        int currentStock = prediction.getCurrentStock();
        double safetyStock = prediction.getSafetyStock();
        double reorderPoint = prediction.getReorderPoint();

        if (currentStock <= safetyStock) {
            assertEquals("HIGH", riskLevel, "Risk should be HIGH when stock <= safety stock");
        } else if (currentStock <= reorderPoint) {
            assertEquals("MEDIUM", riskLevel, "Risk should be MEDIUM when stock <= reorder point");
        } else {
            assertEquals("LOW", riskLevel, "Risk should be LOW when stock > reorder point");
        }
    }

    @Test
    void inventoryPrediction_ShouldHandleZeroDemandGracefully() {
        // Given 
        List<StockEntry> noDemandHistory = Arrays.asList(
                createStockEntry(1L, LocalDateTime.now().minusDays(5), "PURCHASE", 10),
                createStockEntry(2L, LocalDateTime.now().minusDays(4), "PURCHASE", 15),
                createStockEntry(3L, LocalDateTime.now().minusDays(3), "PURCHASE", 20),
                createStockEntry(4L, LocalDateTime.now().minusDays(2), "PURCHASE", 5),
                createStockEntry(5L, LocalDateTime.now().minusDays(1), "PURCHASE", 8)
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockEntryRepository.findByProduct_Id(1L)).thenReturn(noDemandHistory);

        // When
        InventoryPrediction prediction = predictionService.predictInventoryWithLinearRegression(1L, 30);

        // Then
        assertNotNull(prediction);
        assertEquals("Simple Average", prediction.getPredictionMethod());
        assertNotNull(prediction.getPredictedDemand());
        assertEquals(Integer.MAX_VALUE, prediction.getStockoutDays()); 
    }
}