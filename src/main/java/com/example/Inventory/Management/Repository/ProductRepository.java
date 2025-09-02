package com.example.Inventory.Management.Repository;

import com.example.Inventory.Management.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// In ProductRepository
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByNameContainingIgnoreCase(String name);
    
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
    
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.name = :categoryName")
    Long countProductsByCategory(@Param("categoryName") String categoryName);

    List<Product> findByCategory_Name(String categoryName);

    @Query("SELECT p FROM Product p WHERE p.quantity < :threshold")
    List<Product> findByQuantityLessThan(@Param("threshold") Integer threshold);
}
