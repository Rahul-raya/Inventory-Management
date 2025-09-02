package com.example.Inventory.Management.Exception;

public class StockEntryNotFoundException extends RuntimeException {
    public StockEntryNotFoundException(Long id) {
        super("Stock entry not found with id: " + id);
    }
}