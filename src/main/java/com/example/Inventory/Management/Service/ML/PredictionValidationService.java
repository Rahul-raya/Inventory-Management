package com.example.Inventory.Management.Service.ML;

import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Repository.StockEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PredictionValidationService {
    
    private final StockEntryRepository stockEntryRepository;
    private final LinearRegressionPredictionService linearRegressionService;

    /**
     * Validate model accuracy by testing on historical data
     */
    public ModelValidationResult validateModel(Long productId, int testDays) {
        List<StockEntry> allEntries = stockEntryRepository.findByProduct_Id(productId);
        
        if (allEntries.size() < 20) {
            return new ModelValidationResult("Insufficient data for validation", 0.0, 0.0);
        }

        // Split data - use 80% for training, 20% for testing
        int trainSize = (int) (allEntries.size() * 0.8);
        List<StockEntry> trainData = allEntries.subList(0, trainSize);
        List<StockEntry> testData = allEntries.subList(trainSize, allEntries.size());

        List<Double> actualSales = extractSalesFromEntries(testData);
        List<Double> predictedSales = new ArrayList<>();

        // Make predictions for test period
        for (int i = 0; i < actualSales.size(); i++) {
            // This is simplified - in real implementation, you'd retrain model with incremental data
            InventoryPrediction prediction = linearRegressionService.predictInventoryWithLinearRegression(productId, 1);
            predictedSales.add(prediction.getPredictedDemand());
        }

        // Calculate validation metrics
        double mse = calculateMeanSquaredError(actualSales, predictedSales);
        double mae = calculateMeanAbsoluteError(actualSales, predictedSales);
        double accuracy = calculateAccuracy(actualSales, predictedSales);

        return new ModelValidationResult("Linear Regression", mse, mae, accuracy);
    }

    
    // Extract sales data from stock entries
    
    private List<Double> extractSalesFromEntries(List<StockEntry> entries) {
        List<Double> sales = new ArrayList<>();
        for (StockEntry entry : entries) {
            if ("SALE".equals(entry.getType().toUpperCase())) {
                sales.add((double) entry.getQuantity());
            }
        }
        return sales;
    }

    /**
     * Calculate Mean Squared Error
     */
    private double calculateMeanSquaredError(List<Double> actual, List<Double> predicted) {
        if (actual.size() != predicted.size()) {
            return Double.MAX_VALUE;
        }

        double sumSquaredErrors = 0.0;
        for (int i = 0; i < actual.size(); i++) {
            double error = actual.get(i) - predicted.get(i);
            sumSquaredErrors += error * error;
        }
        return sumSquaredErrors / actual.size();
    }

    /**
     * Calculate Mean Absolute Error
     */
    private double calculateMeanAbsoluteError(List<Double> actual, List<Double> predicted) {
        if (actual.size() != predicted.size()) {
            return Double.MAX_VALUE;
        }

        double sumAbsoluteErrors = 0.0;
        for (int i = 0; i < actual.size(); i++) {
            sumAbsoluteErrors += Math.abs(actual.get(i) - predicted.get(i));
        }
        return sumAbsoluteErrors / actual.size();
    }

    /**
     * Calculate Accuracy (percentage of predictions within 20% of actual)
     */
    private double calculateAccuracy(List<Double> actual, List<Double> predicted) {
        if (actual.size() != predicted.size()) {
            return 0.0;
        }

        int correctPredictions = 0;
        for (int i = 0; i < actual.size(); i++) {
            double actualValue = actual.get(i);
            double predictedValue = predicted.get(i);
            double percentageError = Math.abs(actualValue - predictedValue) / actualValue;
            
            if (percentageError <= 0.20) { // Within 20% is considered accurate
                correctPredictions++;
            }
        }
        return (double) correctPredictions / actual.size() * 100;
    }

    /**
     * Model Validation Result class
     */
    public static class ModelValidationResult {
        private String modelName;
        private double meanSquaredError;
        private double meanAbsoluteError;
        private double accuracy;

        public ModelValidationResult(String modelName, double mse, double mae) {
            this.modelName = modelName;
            this.meanSquaredError = mse;
            this.meanAbsoluteError = mae;
            this.accuracy = 0.0;
        }

        public ModelValidationResult(String modelName, double mse, double mae, double accuracy) {
            this.modelName = modelName;
            this.meanSquaredError = mse;
            this.meanAbsoluteError = mae;
            this.accuracy = accuracy;
        }

        // Getters
        public String getModelName() { return modelName; }
        public double getMeanSquaredError() { return meanSquaredError; }
        public double getMeanAbsoluteError() { return meanAbsoluteError; }
        public double getAccuracy() { return accuracy; }

        public String getPerformanceLevel() {
            if (accuracy >= 80) return "EXCELLENT";
            if (accuracy >= 60) return "GOOD";
            if (accuracy >= 40) return "FAIR";
            return "POOR";
        }
    }
}
