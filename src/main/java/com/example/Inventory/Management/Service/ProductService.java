package com.example.Inventory.Management.Service;

import com.example.Inventory.Management.Entity.Product;
import java.util.List;

public interface ProductService {
    Product saveProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);
    Product getProductById(Long id);
    List<Product> getAllProducts();
    List<Product> searchProductsByName(String name);
    List<Product> getProductsByCategory(String categoryName);
    List<Product> getLowStockProducts(Integer threshold);
}
