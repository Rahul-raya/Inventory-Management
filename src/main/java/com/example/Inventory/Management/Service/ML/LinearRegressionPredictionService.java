package com.example.Inventory.Management.Service.ML;

import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Repository.ProductRepository;
import com.example.Inventory.Management.Repository.StockEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
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
        
        if (earliestDate == null) {
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
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        // Calculate sums
        for (DataPoint point : dataPoints) {
            sumX += point.x;
            sumY += point.y;
            sumXY += point.x * point.y;
            sumX2 += point.x * point.x;
        }
        
        // Calculate slope (b1) and intercept (b0)
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        // Calculate R-squared
        double meanY = sumY / n;
        double totalSumSquares = 0;
        double residualSumSquares = 0;
        
        for (DataPoint point : dataPoints) {
            double predicted = intercept + slope * point.x;
            totalSumSquares += Math.pow(point.y - meanY, 2);
            residualSumSquares += Math.pow(point.y - predicted, 2);
        }
        
        double rSquared = 1 - (residualSumSquares / totalSumSquares);
        
        return new LinearRegressionModel(intercept, slope, rSquared);
    }

    /**
     * Calculate safety stock using regression residuals
     */
    private double calculateSafetyStockFromRegression(List<DataPoint> dataPoints, LinearRegressionModel model) {
        if (dataPoints.size() < 2) return 0.0;
        
        // Calculate residuals (actual - predicted)
        List<Double> residuals = new ArrayList<>();
        for (DataPoint point : dataPoints) {
            double predicted = model.predict(point.x);
            residuals.add(Math.abs(point.y - predicted));
        }
        
        // Calculate standard deviation of residuals
        double meanResidual = 0.0;
        for (Double residual : residuals) {
            meanResidual += residual;
        }
        meanResidual = meanResidual / residuals.size();
        
        double variance = 0.0;
        for (Double residual : residuals) {
            variance += Math.pow(residual - meanResidual, 2);
        }
        variance = variance / residuals.size();
        
        double stdDev = Math.sqrt(variance);
        
        // Safety stock = Z-score * std dev * sqrt(lead time)
        return 1.65 * stdDev * Math.sqrt(7); // 95% service level, 7 days lead time
    }

    /**
     * Calculate reorder point
     */
    private double calculateReorderPoint(double dailyDemand, double safetyStock) {
        double leadTimeDays = 7.0;
        return (dailyDemand * leadTimeDays) + safetyStock;
    }

    /**
     * Calculate optimal order quantity
     */
    private double calculateOptimalOrderQuantity(double predictedDemand, Product product) {
        double annualDemand = predictedDemand * 12;
        double orderCost = 100.0;
        double holdingCostRate = 0.25;
        double holdingCost = product.getPrice() * holdingCostRate;
        
        if (holdingCost <= 0) holdingCost = 1.0;
        
        return Math.sqrt((2 * annualDemand * orderCost) / holdingCost);
    }

    /**
     * Simple prediction for limited data
     */
    private InventoryPrediction getSimplePrediction(Product product, int daysToPredict) {
        double simpleDemand = Math.max(1.0, product.getQuantity() * 0.1);
        
        return InventoryPrediction.builder()
                .productId(product.getId())
                .productName(product.getName())
                .currentStock(product.getQuantity())
                .predictedDemand(simpleDemand)
                .safetyStock(simpleDemand * 0.2)
                .reorderPoint(simpleDemand * 1.5)
                .optimalOrderQuantity(simpleDemand * 2)
                .predictionDate(LocalDateTime.now())
                .daysAhead(daysToPredict)
                .predictionMethod("Simple Average")
                .confidence(0.5)
                .build();
    }


    public InventoryPrediction predictWithMultipleFeatures(Long productId, int daysToPredict) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<StockEntry> stockHistory = stockEntryRepository.findByProduct_Id(productId);
        
        if (stockHistory.size() < 10) {
            return predictInventoryWithLinearRegression(productId, daysToPredict);
        }

        List<MultiFeatureDataPoint> dataPoints = prepareMultiFeatureData(stockHistory);
        MultipleLinearRegressionModel model = trainMultipleRegression(dataPoints);
        
        // Predict using current features
        double currentTrend = dataPoints.size();
        int currentSeason = getCurrentSeason();
        double currentPrice = product.getPrice();
        
        double predictedDailyDemand = model.predict(currentTrend, currentSeason, currentPrice);
        double predictedTotalDemand = Math.max(0, predictedDailyDemand * daysToPredict);
        
        return InventoryPrediction.builder()
                .productId(productId)
                .productName(product.getName())
                .currentStock(product.getQuantity())
                .predictedDemand(predictedTotalDemand)
                .safetyStock(predictedTotalDemand * 0.15)
                .reorderPoint(predictedDailyDemand * 7 + predictedTotalDemand * 0.15)
                .optimalOrderQuantity(calculateOptimalOrderQuantity(predictedTotalDemand, product))
                .predictionDate(LocalDateTime.now())
                .daysAhead(daysToPredict)
                .predictionMethod("Multiple Linear Regression")
                .confidence(model.getRSquared())
                .build();
    }

    /**
     * Prepare data for multiple regression
     */
    private List<MultiFeatureDataPoint> prepareMultiFeatureData(List<StockEntry> stockHistory) {
        Map<String, Double> dailySales = new HashMap<>();
        
        for (StockEntry entry : stockHistory) {
            if ("SALE".equals(entry.getType().toUpperCase())) {
                String dateKey = entry.getDate().toLocalDate().toString();
                dailySales.put(dateKey, dailySales.getOrDefault(dateKey, 0.0) + entry.getQuantity());
            }
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

    /**
     * Train Multiple Linear Regression
     */
    private MultipleLinearRegressionModel trainMultipleRegression(List<MultiFeatureDataPoint> dataPoints) {
       
        int n = dataPoints.size();
        double sumY = 0, sumX1 = 0, sumX2 = 0, sumX3 = 0;
        double sumX1Y = 0, sumX2Y = 0, sumX3Y = 0;
        
        for (MultiFeatureDataPoint point : dataPoints) {
            sumY += point.sales;
            sumX1 += point.trend;
            sumX2 += point.season;
            sumX3 += point.price;
            sumX1Y += point.trend * point.sales;
            sumX2Y += point.season * point.sales;
            sumX3Y += point.price * point.sales;
        }
        
        // Simplified coefficients calculation
        double meanY = sumY / n;
        double meanX1 = sumX1 / n;
        double meanX2 = sumX2 / n;
        double meanX3 = sumX3 / n;
        
        double beta1 = (sumX1Y - n * meanX1 * meanY) / (sumX1 * meanX1 - n * meanX1 * meanX1);
        double beta2 = (sumX2Y - n * meanX2 * meanY) / (sumX2 * meanX2 - n * meanX2 * meanX2);
        double beta3 = (sumX3Y - n * meanX3 * meanY) / (sumX3 * meanX3 - n * meanX3 * meanX3);
        double beta0 = meanY - beta1 * meanX1 - beta2 * meanX2 - beta3 * meanX3;
        
        // Calculate R-squared
        double totalSumSquares = 0;
        double residualSumSquares = 0;
        
        for (MultiFeatureDataPoint point : dataPoints) {
            double predicted = beta0 + beta1 * point.trend + beta2 * point.season + beta3 * point.price;
            totalSumSquares += Math.pow(point.sales - meanY, 2);
            residualSumSquares += Math.pow(point.sales - predicted, 2);
        }
        
        double rSquared = 1 - (residualSumSquares / totalSumSquares);
        
        return new MultipleLinearRegressionModel(beta0, beta1, beta2, beta3, rSquared);
    }

    private int getCurrentSeason() {
        return LocalDateTime.now().getMonthValue();
    }

    /**
     * Data Point for simple linear regression
     */
    private static class DataPoint {
        double x, y;
        
        DataPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Data Point for multiple regression
     */
    private static class MultiFeatureDataPoint {
        double trend, season, price, sales;
        
        MultiFeatureDataPoint(double trend, double season, double price, double sales) {
            this.trend = trend;
            this.season = season;
            this.price = price;
            this.sales = sales;
        }
    }

    /**
     * Simple Linear Regression Model
     */
    private static class LinearRegressionModel {
        private double intercept, slope, rSquared;
        
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

    /**
     * Multiple Linear Regression Model
     */
    private static class MultipleLinearRegressionModel {
        private double beta0, beta1, beta2, beta3, rSquared;
        
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