package com.example.Inventory.Management.Service.ML;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryPrediction {
    
    private Long productId;
    private String productName;
    private Integer currentStock;
    private Double predictedDemand;
    private Double safetyStock;
    private Double reorderPoint;
    private Double optimalOrderQuantity;
    private LocalDateTime predictionDate;
    private Integer daysAhead;
    private String predictionMethod;
    private Double confidence;
    
    // Calculated properties
    public boolean isRestockNeeded() {
        return currentStock <= reorderPoint;
    }
    
    public Double getRecommendedOrderQuantity() {
        if (isRestockNeeded()) {
            return Math.max(optimalOrderQuantity, predictedDemand - currentStock);
        }
        return 0.0;
    }
    
    public String getRiskLevel() {
        if (currentStock <= safetyStock) {
            return "HIGH";
        } else if (currentStock <= reorderPoint) {
            return "MEDIUM";
        }
        return "LOW";
    }
    
    public Integer getStockoutDays() {
        if (predictedDemand <= 0) return Integer.MAX_VALUE;
        return (int) Math.ceil(currentStock / (predictedDemand / daysAhead));
    }
}
