package com.example.Inventory.Management.Util;

import com.example.Inventory.Management.Entity.*;
import com.example.Inventory.Management.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private StockEntryRepository stockEntryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() > 0) {
            return; 
        }

        // Create sample category
        Category electronics = Category.builder()
            .name("Electronics")
            .description("Electronic items")
            .build();
        categoryRepository.save(electronics);

        // Create sample product
        Product laptop = Product.builder()
            .name("Laptop")
            .price(50000.0)
            .quantity(100)
            .category(electronics)
            .build();
        productRepository.save(laptop);

        // Create sample stock entries 
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 30; i >= 1; i--) {
            LocalDateTime date = now.minusDays(i);
            int salesQty = random.nextInt(5) + 1; // 1-5 units sold per day
            
            StockEntry sale = StockEntry.builder()
                .product(laptop)
                .quantity(salesQty)
                .type("SALE")
                .date(date)
                .build();
            stockEntryRepository.save(sale);
        }
    }
}