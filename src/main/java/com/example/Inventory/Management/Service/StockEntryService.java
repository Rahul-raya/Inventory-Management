package com.example.Inventory.Management.Service;

import com.example.Inventory.Management.Entity.StockEntry;
import java.util.List;

public interface StockEntryService {
    StockEntry addStockEntry(StockEntry stockEntry);
    List<StockEntry> getStockEntriesByProduct(Long productId);
    StockEntry updateStockEntry(Long id, StockEntry stockEntry);
    StockEntry getStockEntryById(Long id);
    void deleteStockEntry(Long id);
}
