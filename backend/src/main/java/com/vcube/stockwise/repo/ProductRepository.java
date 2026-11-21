package com.vcube.stockwise.repo;
import com.vcube.stockwise.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,Long> {
 Optional<Product> findBySkuIgnoreCase(String sku);
 boolean existsBySkuIgnoreCase(String sku);
 boolean existsBySupplier_Id(Long supplierId);
}
