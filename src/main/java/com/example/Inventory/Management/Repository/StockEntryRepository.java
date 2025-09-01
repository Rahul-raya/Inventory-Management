package com.example.Inventory.Management.Repository;

import com.example.Inventory.Management.Entity.StockEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockEntryRepository extends JpaRepository<StockEntry, Long> {

    List<StockEntry> findByProduct_Id(Long productId);

    List<StockEntry> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
