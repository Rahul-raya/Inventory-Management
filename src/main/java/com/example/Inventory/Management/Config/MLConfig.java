package com.example.Inventory.Management.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "inventory.ml")
public class MLConfig {
    
    private double smoothingFactor = 0.3;
    private int minHistoryDays = 7;
    private double serviceLevel = 0.95; // 95% service level
    private double leadTimeDays = 7.0;
    private double orderingCost = 100.0;
    private double holdingCostRate = 0.25;
    private int defaultPredictionDays = 30;
    
    // Getters and setters
    public double getSmoothingFactor() {
        return smoothingFactor;
    }
    
    public void setSmoothingFactor(double smoothingFactor) {
        this.smoothingFactor = smoothingFactor;
    }
    
    public int getMinHistoryDays() {
        return minHistoryDays;
    }
    
    public void setMinHistoryDays(int minHistoryDays) {
        this.minHistoryDays = minHistoryDays;
    }
    
    public double getServiceLevel() {
        return serviceLevel;
    }
    
    public void setServiceLevel(double serviceLevel) {
        this.serviceLevel = serviceLevel;
    }
    
    public double getLeadTimeDays() {
        return leadTimeDays;
    }
    
    public void setLeadTimeDays(double leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }
    
    public double getOrderingCost() {
        return orderingCost;
    }
    
    public void setOrderingCost(double orderingCost) {
        this.orderingCost = orderingCost;
    }
    
    public double getHoldingCostRate() {
        return holdingCostRate;
    }
    
    public void setHoldingCostRate(double holdingCostRate) {
        this.holdingCostRate = holdingCostRate;
    }
    
    public int getDefaultPredictionDays() {
        return defaultPredictionDays;
    }
    
    public void setDefaultPredictionDays(int defaultPredictionDays) {
        this.defaultPredictionDays = defaultPredictionDays;
    }
    
    public double getZScore() {
        // Convert service level to Z-score
        if (serviceLevel >= 0.99) return 2.33;
        if (serviceLevel >= 0.95) return 1.65;
        if (serviceLevel >= 0.90) return 1.28;
        return 1.0; // Default
    }
}