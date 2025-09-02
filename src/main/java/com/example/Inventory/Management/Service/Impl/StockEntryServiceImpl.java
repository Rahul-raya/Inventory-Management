package com.example.Inventory.Management.Service.Impl;

import com.example.Inventory.Management.Entity.Product;
import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Exception.InsufficientStockException;
import com.example.Inventory.Management.Exception.ProductNotFoundException;
import com.example.Inventory.Management.Exception.StockEntryNotFoundException;
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
        Long productId = stockEntry.getProduct().getId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        String type = stockEntry.getType().toUpperCase();
        if (!type.equals("SALE") && !type.equals("PURCHASE")) {
            throw new RuntimeException("Invalid stock entry type. Must be PURCHASE or SALE.");
        }
        if ("SALE".equals(type)) {
            if (product.getQuantity() < stockEntry.getQuantity()) {
                throw new InsufficientStockException(
                    product.getName(), 
                    product.getQuantity(), 
                    stockEntry.getQuantity()
                );
            }
            product.setQuantity(product.getQuantity() - stockEntry.getQuantity());
        } else if ("PURCHASE".equals(type)) {
            product.setQuantity(product.getQuantity() + stockEntry.getQuantity());
        }

        productRepository.save(product);
        stockEntry.setProduct(product);
        stockEntry.setType(type);
        
        return stockEntryRepository.save(stockEntry);
    }

    @Override
    public List<StockEntry> getStockEntriesByProduct(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        
        return stockEntryRepository.findByProduct_Id(productId);
    }

    @Override
    public StockEntry updateStockEntry(Long id, StockEntry stockEntry) {
        StockEntry existingEntry = getStockEntryById(id);
        Product oldProduct = existingEntry.getProduct();
        String newType = stockEntry.getType().toUpperCase();
        if (!newType.equals("SALE") && !newType.equals("PURCHASE")) {
            throw new RuntimeException("Invalid stock entry type. Must be PURCHASE or SALE.");
        }
        if ("SALE".equals(existingEntry.getType().toUpperCase())) {
            oldProduct.setQuantity(oldProduct.getQuantity() + existingEntry.getQuantity());
        } else if ("PURCHASE".equals(existingEntry.getType().toUpperCase())) {
            oldProduct.setQuantity(oldProduct.getQuantity() - existingEntry.getQuantity());
        }
        Long newProductId = stockEntry.getProduct().getId();
        Product newProduct = productRepository.findById(newProductId)
                .orElseThrow(() -> new ProductNotFoundException(newProductId));


        if ("SALE".equals(newType)) {
            if (newProduct.getQuantity() < stockEntry.getQuantity()) {
                throw new InsufficientStockException(
                    newProduct.getName(), 
                    newProduct.getQuantity(), 
                    stockEntry.getQuantity()
                );
            }
            newProduct.setQuantity(newProduct.getQuantity() - stockEntry.getQuantity());
        } else if ("PURCHASE".equals(newType)) {
            newProduct.setQuantity(newProduct.getQuantity() + stockEntry.getQuantity());
        }

        productRepository.save(oldProduct);
        if (!oldProduct.getId().equals(newProduct.getId())) {
            productRepository.save(newProduct);
        }

        existingEntry.setProduct(newProduct);
        existingEntry.setQuantity(stockEntry.getQuantity());
        existingEntry.setType(newType);
        existingEntry.setDate(stockEntry.getDate());

        return stockEntryRepository.save(existingEntry);
    }

    @Override
    public StockEntry getStockEntryById(Long id) {
        return stockEntryRepository.findById(id)
                .orElseThrow(() -> new StockEntryNotFoundException(id));
    }

    @Override
    public void deleteStockEntry(Long id) {
        StockEntry stockEntry = getStockEntryById(id);
        Product product = stockEntry.getProduct();
        if ("SALE".equals(stockEntry.getType().toUpperCase())) {
            product.setQuantity(product.getQuantity() + stockEntry.getQuantity());
        } else if ("PURCHASE".equals(stockEntry.getType().toUpperCase())) {
            if (product.getQuantity() < stockEntry.getQuantity()) {
                throw new InsufficientStockException(
                    product.getName(), 
                    product.getQuantity(), 
                    stockEntry.getQuantity()
                );
            }
            product.setQuantity(product.getQuantity() - stockEntry.getQuantity());
        }
        
        productRepository.save(product);
        stockEntryRepository.deleteById(id);
    }

    @Override
    public List<StockEntry> getAllStockEntries() {
        return stockEntryRepository.findAll();
    }
}