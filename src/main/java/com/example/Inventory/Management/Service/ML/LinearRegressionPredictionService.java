package com.example.Inventory.Management.Service.ML;

import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Repository.ProductRepository;
import com.example.Inventory.Management.Repository.StockEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LinearRegressionPredictionService {

    private final ProductRepository productRepository;
    private final StockEntryRepository stockEntryRepository;

    /**
     * Main prediction method using Linear Regression
     */
    public InventoryPrediction predictInventoryWithLinearRegression(Long productId, int daysToPredict) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<StockEntry> stockHistory = stockEntryRepository.findByProduct_Id(productId);
        
        if (stockHistory.size() < 5) {
            return getSimplePrediction(product, daysToPredict);
        }

        // Prepare time series data
        List<DataPoint> salesDataPoints = prepareDailySalesData(stockHistory);
        
        if (salesDataPoints.size() < 3) {
            return getSimplePrediction(product, daysToPredict);
        }

        // Apply Linear Regression
        LinearRegressionModel model = trainLinearRegression(salesDataPoints);
        
        // Predict future demand
        double predictedDailyDemand = model.predict(salesDataPoints.size() + daysToPredict / 2.0);
        double predictedTotalDemand = Math.max(0, predictedDailyDemand * daysToPredict);
        
        // Calculate additional metrics
        double safetyStock = calculateSafetyStockFromRegression(salesDataPoints, model);
        double reorderPoint = calculateReorderPoint(predictedDailyDemand, safetyStock);
        double optimalOrderQty = calculateOptimalOrderQuantity(predictedTotalDemand, product);

        return InventoryPrediction.builder()
                .productId(productId)
                .productName(product.getName())
                .currentStock(product.getQuantity())
                .predictedDemand(predictedTotalDemand)
                .safetyStock(safetyStock)
                .reorderPoint(reorderPoint)
                .optimalOrderQuantity(optimalOrderQty)
                .predictionDate(LocalDateTime.now())
                .daysAhead(daysToPredict)
                .predictionMethod("Linear Regression")
                .confidence(model.getRSquared())
                .build();
    }

    
    //FIXED: Multiple features prediction method
    
    public InventoryPrediction predictWithMultipleFeatures(Long productId, int daysToPredict) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<StockEntry> stockHistory = stockEntryRepository.findByProduct_Id(productId);
        
        // FIXED: Changed condition from < 10 to <= 10 to match the test expectation
        if (stockHistory.size() <= 10) {
            // Fall back to simple linear regression for insufficient data
            return predictInventoryWithLinearRegression(productId, daysToPredict);
        }

        // Prepare data for multiple regression
        List<MultiFeatureDataPoint> dataPoints = prepareMultiFeatureData(stockHistory);
        
        // FIXED: Added additional check for prepared data points
        if (dataPoints.size() < 5) {
            return predictInventoryWithLinearRegression(productId, daysToPredict);
        }
        
        MultipleLinearRegressionModel model = trainMultipleRegression(dataPoints);
        
        // Predict using current features
        double currentTrend = dataPoints.size();
        int currentSeason = getCurrentSeason();
        double currentPrice = product.getPrice();
        
        double predictedDailyDemand = model.predict(currentTrend, currentSeason, currentPrice);
        double predictedTotalDemand = Math.max(0, predictedDailyDemand * daysToPredict);
        
        // Calculate metrics using the predicted demand
        double safetyStock = predictedTotalDemand * 0.15;
        double reorderPoint = predictedDailyDemand * 7 + safetyStock;
        double optimalOrderQty = calculateOptimalOrderQuantity(predictedTotalDemand, product);
        
        return InventoryPrediction.builder()
                .productId(productId)
                .productName(product.getName())
                .currentStock(product.getQuantity())
                .predictedDemand(predictedTotalDemand)
                .safetyStock(safetyStock)
                .reorderPoint(reorderPoint)
                .optimalOrderQuantity(optimalOrderQty)
                .predictionDate(LocalDateTime.now())
                .daysAhead(daysToPredict)
                .predictionMethod("Multiple Linear Regression") // This was the issue!
                .confidence(model.getRSquared())
                .build();
    }

    /**
     * Prepare daily sales data points for regression
     */
    private List<DataPoint> prepareDailySalesData(List<StockEntry> stockHistory) {
        Map<String, Double> dailySales = new HashMap<>();
        LocalDateTime earliestDate = null;
        
        // Group sales by date
        for (StockEntry entry : stockHistory) {
            if ("SALE".equals(entry.getType().toUpperCase())) {
                String dateKey = entry.getDate().toLocalDate().toString();
                dailySales.put(dateKey, dailySales.getOrDefault(dateKey, 0.0) + entry.getQuantity());
                
                if (earliestDate == null || entry.getDate().isBefore(earliestDate)) {
                    earliestDate = entry.getDate();
                }
            }
        }
        
        if (earliestDate == null || dailySales.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Convert to DataPoints with time index
        List<DataPoint> dataPoints = new ArrayList<>();
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(dailySales.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        
        for (int i = 0; i < sortedEntries.size(); i++) {
            dataPoints.add(new DataPoint(i + 1, sortedEntries.get(i).getValue()));
        }
        
        return dataPoints;
    }

    /**
     * Train Linear Regression Model
     */
    private LinearRegressionModel trainLinearRegression(List<DataPoint> dataPoints) {
        int n = dataPoints.size();
        if (n < 2) {
            return new LinearRegressionModel(1.0, 0.0, 0.0); // Default model
        }
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        // Calculate sums
        for (DataPoint point : dataPoints) {
            sumX += point.x;
            sumY += point.y;
            sumXY += point.x * point.y;
            sumX2 += point.x * point.x;
        }
        
        // Calculate slope (b1) and intercept (b0)
        double denominator = n * sumX2 - sumX * sumX;
        double slope = 0.0;
        double intercept = sumY / n;
        
        if (Math.abs(denominator) > 1e-10) { // Avoid division by zero
            slope = (n * sumXY - sumX * sumY) / denominator;
            intercept = (sumY - slope * sumX) / n;
        }
        
        // Calculate R-squared
        double meanY = sumY / n;
        double totalSumSquares = 0;
        double residualSumSquares = 0;
        
        for (DataPoint point : dataPoints) {
            double predicted = intercept + slope * point.x;
            totalSumSquares += Math.pow(point.y - meanY, 2);
            residualSumSquares += Math.pow(point.y - predicted, 2);
        }
        
        double rSquared = 0.0;
        if (totalSumSquares > 0) {
            rSquared = Math.max(0.0, 1 - (residualSumSquares / totalSumSquares));
        }
        
        return new LinearRegressionModel(intercept, slope, rSquared);
    }

    
    // Calculate safety stock using regression residuals
    
    private double calculateSafetyStockFromRegression(List<DataPoint> dataPoints, LinearRegressionModel model) {
        if (dataPoints.size() < 2) return 1.0; // Default safety stock
        
        // Calculate residuals (actual - predicted)
        List<Double> residuals = new ArrayList<>();
        for (DataPoint point : dataPoints) {
            double predicted = model.predict(point.x);
            residuals.add(Math.abs(point.y - predicted));
        }
        
        // Calculate standard deviation of residuals
        double meanResidual = residuals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double variance = 0.0;
        for (Double residual : residuals) {
            variance += Math.pow(residual - meanResidual, 2);
        }
        variance = variance / residuals.size();
        
        double stdDev = Math.sqrt(variance);
        
        // Safety stock = Z-score * std dev * sqrt(lead time)
        return Math.max(1.0, 1.65 * stdDev * Math.sqrt(7)); // 95% service level, 7 days lead time
    }

    /**
     * Calculate reorder point
     */
    private double calculateReorderPoint(double dailyDemand, double safetyStock) {
        double leadTimeDays = 7.0;
        return Math.max(safetyStock, (dailyDemand * leadTimeDays) + safetyStock);
    }

    
    //Calculate optimal order quantity using EOQ formula
    
    private double calculateOptimalOrderQuantity(double predictedDemand, Product product) {
        double annualDemand = Math.max(1.0, predictedDemand * 12); // Ensure positive
        double orderCost = 100.0;
        double holdingCostRate = 0.25;
        double holdingCost = Math.max(1.0, product.getPrice() * holdingCostRate);
        return Math.sqrt((2 * annualDemand * orderCost) / holdingCost);
    }

    
    //Simple prediction for limited data
    
    private InventoryPrediction getSimplePrediction(Product product, int daysToPredict) {
        double simpleDemand = Math.max(1.0, product.getQuantity() * 0.1);
        double safetyStock = simpleDemand * 0.2;
        double reorderPoint = simpleDemand * 1.5;
        double optimalOrderQty = simpleDemand * 2;
        
        return InventoryPrediction.builder()
                .productId(product.getId())
                .productName(product.getName())
                .currentStock(product.getQuantity())
                .predictedDemand(simpleDemand)
                .safetyStock(safetyStock)
                .reorderPoint(reorderPoint)
                .optimalOrderQuantity(optimalOrderQty)
                .predictionDate(LocalDateTime.now())
                .daysAhead(daysToPredict)
                .predictionMethod("Simple Average")
                .confidence(0.5)
                .build();
    }

    
    // Prepare data for multiple regression - IMPROVED
    
    private List<MultiFeatureDataPoint> prepareMultiFeatureData(List<StockEntry> stockHistory) {
        Map<String, Double> dailySales = new HashMap<>();
        
        // Group sales by date
        for (StockEntry entry : stockHistory) {
            if ("SALE".equals(entry.getType().toUpperCase())) {
                String dateKey = entry.getDate().toLocalDate().toString();
                dailySales.put(dateKey, dailySales.getOrDefault(dateKey, 0.0) + entry.getQuantity());
            }
        }
        
        if (dailySales.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<MultiFeatureDataPoint> dataPoints = new ArrayList<>();
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(dailySales.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        
        for (int i = 0; i < sortedEntries.size(); i++) {
            int season = (i % 12) + 1; 
            double trend = i + 1; 
            double price = 10.0; 
            double sales = sortedEntries.get(i).getValue();
            
            dataPoints.add(new MultiFeatureDataPoint(trend, season, price, sales));
        }
        
        return dataPoints;
    }


    // Train Multiple Linear Regression 
    private MultipleLinearRegressionModel trainMultipleRegression(List<MultiFeatureDataPoint> dataPoints) {
        int n = dataPoints.size();
        if (n < 4) {
            // Not enough data for multiple regression, return default model
            return new MultipleLinearRegressionModel(1.0, 0.1, 0.0, 0.0, 0.0);
        }
        
        // Calculate means
        double meanY = dataPoints.stream().mapToDouble(p -> p.sales).average().orElse(0.0);
        double meanX1 = dataPoints.stream().mapToDouble(p -> p.trend).average().orElse(0.0);
        double meanX2 = dataPoints.stream().mapToDouble(p -> p.season).average().orElse(0.0);
        double meanX3 = dataPoints.stream().mapToDouble(p -> p.price).average().orElse(0.0);
        
        // Simplified multiple regression using least squares approximation
        double sumX1Dev2 = 0, sumX2Dev2 = 0, sumX3Dev2 = 0;
        double sumX1Y = 0, sumX2Y = 0, sumX3Y = 0;
        
        for (MultiFeatureDataPoint point : dataPoints) {
            double x1Dev = point.trend - meanX1;
            double x2Dev = point.season - meanX2;
            double x3Dev = point.price - meanX3;
            double yDev = point.sales - meanY;
            
            sumX1Dev2 += x1Dev * x1Dev;
            sumX2Dev2 += x2Dev * x2Dev;
            sumX3Dev2 += x3Dev * x3Dev;
            
            sumX1Y += x1Dev * yDev;
            sumX2Y += x2Dev * yDev;
            sumX3Y += x3Dev * yDev;
        }
        
        // Calculate coefficients (simplified approach)
        double beta1 = (sumX1Dev2 > 1e-10) ? sumX1Y / sumX1Dev2 : 0.0;
        double beta2 = (sumX2Dev2 > 1e-10) ? sumX2Y / sumX2Dev2 : 0.0;
        double beta3 = (sumX3Dev2 > 1e-10) ? sumX3Y / sumX3Dev2 : 0.0;
        double beta0 = meanY - beta1 * meanX1 - beta2 * meanX2 - beta3 * meanX3;
        
        // Calculate R-squared
        double totalSumSquares = 0;
        double residualSumSquares = 0;
        
        for (MultiFeatureDataPoint point : dataPoints) {
            double predicted = beta0 + beta1 * point.trend + beta2 * point.season + beta3 * point.price;
            totalSumSquares += Math.pow(point.sales - meanY, 2);
            residualSumSquares += Math.pow(point.sales - predicted, 2);
        }
        
        double rSquared = 0.0;
        if (totalSumSquares > 0) {
            rSquared = Math.max(0.0, 1 - (residualSumSquares / totalSumSquares));
        }
        
        return new MultipleLinearRegressionModel(beta0, beta1, beta2, beta3, rSquared);
    }

    private int getCurrentSeason() {
        return LocalDateTime.now().getMonthValue();
    }

     // Data Point for simple linear regression

    private static class DataPoint {
        final double x, y;
        
        DataPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // Data Point for multiple regression    
    private static class MultiFeatureDataPoint {
        final double trend, season, price, sales;
        
        MultiFeatureDataPoint(double trend, double season, double price, double sales) {
            this.trend = trend;
            this.season = season;
            this.price = price;
            this.sales = sales;
        }
    }

    //Simple Linear Regression Model
    private static class LinearRegressionModel {
        private final double intercept, slope, rSquared;
        
        LinearRegressionModel(double intercept, double slope, double rSquared) {
            this.intercept = intercept;
            this.slope = slope;
            this.rSquared = rSquared;
        }
        
        double predict(double x) {
            return intercept + slope * x;
        }
        
        double getRSquared() {
            return rSquared;
        }
    }

    //Multiple Linear Regression Model

    private static class MultipleLinearRegressionModel {
        private final double beta0, beta1, beta2, beta3, rSquared;
        
        MultipleLinearRegressionModel(double beta0, double beta1, double beta2, double beta3, double rSquared) {
            this.beta0 = beta0;
            this.beta1 = beta1;
            this.beta2 = beta2;
            this.beta3 = beta3;
            this.rSquared = rSquared;
        }
        
        double predict(double trend, double season, double price) {
            return beta0 + beta1 * trend + beta2 * season + beta3 * price;
        }
        
        double getRSquared() {
            return rSquared;
        }
    }
}