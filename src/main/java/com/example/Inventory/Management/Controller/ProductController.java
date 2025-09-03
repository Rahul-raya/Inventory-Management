package com.example.Inventory.Management.Controller;

import com.example.Inventory.Management.Service.ML.LinearRegressionPredictionService;
import com.example.Inventory.Management.Service.ML.PredictionValidationService;
import com.example.Inventory.Management.Service.ML.InventoryPrediction;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;

import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Autowired
    private LinearRegressionPredictionService linearRegressionService;

    @Autowired
    private PredictionValidationService validationService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    public Product createProduct(@Valid @RequestBody Product product) {
        return productService.saveProduct(product);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "Product deleted successfully!";
    }

    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String name) {
        return productService.searchProductsByName(name);
    }

    @GetMapping("/category/{categoryName}")
    public List<Product> getProductsByCategory(@PathVariable String categoryName) {
        return productService.getProductsByCategory(categoryName);
    }

    @GetMapping("/low-stock")
    public List<Product> getLowStockProducts(@RequestParam Integer threshold) {
        return productService.getLowStockProducts(threshold);
    }

    @GetMapping("/{id}/prediction")
    public InventoryPrediction getInventoryPrediction(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days) {
        return linearRegressionService.predictInventoryWithLinearRegression(id, days);
    }

    @GetMapping("/{id}/prediction/multiple-regression")
    public InventoryPrediction getMultipleRegressionPrediction(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days) {
        return linearRegressionService.predictWithMultipleFeatures(id, days);
    }

    @GetMapping("/{id}/validate-model")
    public PredictionValidationService.ModelValidationResult validatePredictionModel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int testDays) {
        return validationService.validateModel(id, testDays);
    }

    @GetMapping("/restock-recommendations")
    public List<InventoryPrediction> getRestockRecommendations() {
        List<Product> allProducts = productService.getAllProducts();
        List<InventoryPrediction> recommendations = new ArrayList<>();

        for (Product product : allProducts) {
            InventoryPrediction prediction = linearRegressionService
                    .predictInventoryWithLinearRegression(product.getId(), 30);
            if (prediction.isRestockNeeded()) {
                recommendations.add(prediction);
            }
        }

        return recommendations;
    }
}