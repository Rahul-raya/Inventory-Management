package com.example.Inventory.Management.Service.Impl;

import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Repository.ProductRepository;
import com.example.Inventory.Management.Repository.StockEntryRepository;
import com.example.Inventory.Management.Service.StockEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockEntryServiceImpl implements StockEntryService {

    private final StockEntryRepository stockEntryRepository;
    private final ProductRepository productRepository;

    @Override
    public StockEntry addStockEntry(StockEntry stockEntry) {
        // Validate product id
        Long productId = stockEntry.getProduct().getId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Handle stock changes
        if ("SALE".equalsIgnoreCase(stockEntry.getType())) {
            if (product.getQuantity() < stockEntry.getQuantity()) {
                throw new RuntimeException("Not enough stock available for product id: " + productId);
            }
            product.setQuantity(product.getQuantity() - stockEntry.getQuantity());
        } else if ("PURCHASE".equalsIgnoreCase(stockEntry.getType())) {
            product.setQuantity(product.getQuantity() + stockEntry.getQuantity());
        } else {
            throw new RuntimeException("Invalid stock entry type. Must be PURCHASE or SALE.");
        }

        productRepository.save(product);
        stockEntry.setProduct(product);
        return stockEntryRepository.save(stockEntry);
    }

    @Override
    public List<StockEntry> getStockEntriesByProduct(Long productId) {
        return stockEntryRepository.findByProduct_Id(productId);
    }

    @Override
    public StockEntry updateStockEntry(Long id, StockEntry stockEntry) {
        StockEntry existingEntry = getStockEntryById(id);

        Long productId = stockEntry.getProduct().getId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        existingEntry.setProduct(product);
        existingEntry.setQuantity(stockEntry.getQuantity());
        existingEntry.setType(stockEntry.getType());
        existingEntry.setDate(stockEntry.getDate());

        return stockEntryRepository.save(existingEntry);
    }

    @Override
    public StockEntry getStockEntryById(Long id) {
        return stockEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock entry not found with id: " + id));
    }

    @Override
    public void deleteStockEntry(Long id) {
        stockEntryRepository.deleteById(id);
    }
}
