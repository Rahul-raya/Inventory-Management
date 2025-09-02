package com.example.Inventory.Management.Controller;

import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Service.StockEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/stock-entries")
@RequiredArgsConstructor
public class StockEntryController {

    private final StockEntryService stockEntryService;

    @GetMapping
    public List<StockEntry> getAllStockEntries() {
        return stockEntryService.getAllStockEntries();
    }

    @GetMapping("/{id}")
    public StockEntry getStockEntryById(@PathVariable Long id) {
        return stockEntryService.getStockEntryById(id);
    }

    @GetMapping("/product/{productId}")
    public List<StockEntry> getStockEntriesByProduct(@PathVariable Long productId) {
        return stockEntryService.getStockEntriesByProduct(productId);
    }

    @PostMapping
    public StockEntry saveStockEntry(@Valid @RequestBody StockEntry stockEntry) {
        return stockEntryService.addStockEntry(stockEntry);
    }

    @PutMapping("/{id}")
    public StockEntry updateStockEntry(@PathVariable Long id, @Valid @RequestBody StockEntry stockEntry) {
        return stockEntryService.updateStockEntry(id, stockEntry);
    }

    @DeleteMapping("/{id}")
    public String deleteStockEntry(@PathVariable Long id) {
        stockEntryService.deleteStockEntry(id);
        return "Stock entry deleted successfully!";
    }
}