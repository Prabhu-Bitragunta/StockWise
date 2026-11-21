package com.vcube.stockwise.repo;
import com.vcube.stockwise.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SupplierRepository extends JpaRepository<Supplier,Long> {
 java.util.Optional<Supplier> findByNameIgnoreCase(String name);
 boolean existsByNameIgnoreCase(String name);
}
