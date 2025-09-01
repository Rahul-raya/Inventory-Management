package com.example.Inventory.Management.Repository;

import com.example.Inventory.Management.Entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Supplier findByName(String name);
}
