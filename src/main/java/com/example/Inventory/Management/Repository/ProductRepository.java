package com.example.Inventory.Management.Repository;

import com.example.Inventory.Management.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategory_Name(String categoryName);

    List<Product> findByQuantityLessThan(Integer threshold);
}
