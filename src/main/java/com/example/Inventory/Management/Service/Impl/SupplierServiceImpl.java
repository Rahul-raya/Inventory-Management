package com.example.Inventory.Management.Service.Impl;

import com.example.Inventory.Management.Entity.Supplier;
import com.example.Inventory.Management.Repository.SupplierRepository;
import com.example.Inventory.Management.Service.SupplierService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    @Override
    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Override
    public void deleteSupplier(Long id) {
        // Consider checking if supplier has associated products before deletion
        Supplier supplier = getSupplierById(id);
        if (!supplier.getProducts().isEmpty()) {
            throw new RuntimeException("Cannot delete supplier that has associated products");
        }
        supplierRepository.deleteById(id);
    }

    @Override
    public Supplier updateSupplier(Long id, Supplier supplier) {
        Supplier existingSupplier = getSupplierById(id);
        existingSupplier.setName(supplier.getName());
        existingSupplier.setContactNumber(supplier.getContactNumber());
        // FIX: Missing email update
        existingSupplier.setEmail(supplier.getEmail());
        return supplierRepository.save(existingSupplier);
    }
}