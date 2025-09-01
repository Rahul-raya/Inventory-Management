package com.example.Inventory.Management.Controller;

import com.example.Inventory.Management.Entity.StockEntry;
import com.example.Inventory.Management.Service.StockEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/stock-entries")
@RequiredArgsConstructor
public class StockEntryController {

    private final StockEntryService stockEntryService;


    @GetMapping("/{id}")
    public StockEntry getStockEntryById(@PathVariable Long id) {
        return stockEntryService.getStockEntryById(id);
    }

    @PostMapping
    public StockEntry saveStockEntry(@RequestBody StockEntry stockEntry) {
        return stockEntryService.addStockEntry(stockEntry);
    }

    @PutMapping("/{id}")
    public StockEntry updateStockEntry(@PathVariable Long id, @RequestBody StockEntry stockEntry) {
        return stockEntryService.updateStockEntry(id, stockEntry);
    }

    @DeleteMapping("/{id}")
    public String deleteStockEntry(@PathVariable Long id) {
        stockEntryService.deleteStockEntry(id);
        return "Stock entry deleted successfully!";
    }
}
