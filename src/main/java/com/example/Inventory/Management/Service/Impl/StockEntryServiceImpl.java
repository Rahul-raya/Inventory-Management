package com.example.Inventory.Management.Service.Impl;

import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Repository.ProductRepository;
import com.example.Inventory.Management.Repository.StockEntryRepository;
import com.example.Inventory.Management.Service.StockEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockEntryServiceImpl implements StockEntryService {

    private final StockEntryRepository stockEntryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
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
    @Transactional
    public StockEntry updateStockEntry(Long id, StockEntry stockEntry) {
        StockEntry existingEntry = getStockEntryById(id);
        Product oldProduct = existingEntry.getProduct();
        
        // Reverse the old stock change
        if ("SALE".equalsIgnoreCase(existingEntry.getType())) {
            oldProduct.setQuantity(oldProduct.getQuantity() + existingEntry.getQuantity());
        } else if ("PURCHASE".equalsIgnoreCase(existingEntry.getType())) {
            oldProduct.setQuantity(oldProduct.getQuantity() - existingEntry.getQuantity());
        }

        // Get the new product (might be different)
        Long newProductId = stockEntry.getProduct().getId();
        Product newProduct = productRepository.findById(newProductId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + newProductId));

        // Apply the new stock change
        if ("SALE".equalsIgnoreCase(stockEntry.getType())) {
            if (newProduct.getQuantity() < stockEntry.getQuantity()) {
                throw new RuntimeException("Not enough stock available for product id: " + newProductId);
            }
            newProduct.setQuantity(newProduct.getQuantity() - stockEntry.getQuantity());
        } else if ("PURCHASE".equalsIgnoreCase(stockEntry.getType())) {
            newProduct.setQuantity(newProduct.getQuantity() + stockEntry.getQuantity());
        } else {
            throw new RuntimeException("Invalid stock entry type. Must be PURCHASE or SALE.");
        }

        // Save products and update entry
        productRepository.save(oldProduct);
        if (!oldProduct.getId().equals(newProduct.getId())) {
            productRepository.save(newProduct);
        }

        existingEntry.setProduct(newProduct);
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
    @Transactional
    public void deleteStockEntry(Long id) {
        StockEntry stockEntry = getStockEntryById(id);
        Product product = stockEntry.getProduct();
        
        // Reverse the stock change when deleting
        if ("SALE".equalsIgnoreCase(stockEntry.getType())) {
            product.setQuantity(product.getQuantity() + stockEntry.getQuantity());
        } else if ("PURCHASE".equalsIgnoreCase(stockEntry.getType())) {
            product.setQuantity(product.getQuantity() - stockEntry.getQuantity());
        }
        
        productRepository.save(product);
        stockEntryRepository.deleteById(id);
    }

    // MISSING METHOD - ADD THIS:
    @Override
    public List<StockEntry> getAllStockEntries() {
        return stockEntryRepository.findAll();
    }
}