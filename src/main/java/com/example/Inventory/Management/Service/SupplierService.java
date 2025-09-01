package com.example.Inventory.Management.Service;

import com.example.Inventory.Management.Entity.Supplier;
import java.util.List;

public interface SupplierService {
    Supplier saveSupplier(Supplier supplier);
    Supplier getSupplierById(Long id);
    List<Supplier> getAllSuppliers();
    void deleteSupplier(Long id);
    Supplier updateSupplier(Long id, Supplier supplier);
}
