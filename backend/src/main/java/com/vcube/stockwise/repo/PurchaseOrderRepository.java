package com.vcube.stockwise.repo;
import com.vcube.stockwise.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder,Long> {
 long countByStatusIn(java.util.Collection<String> statuses);
 java.util.List<PurchaseOrder> findBySupplier_Id(Long supplierId);
 @Override @EntityGraph(attributePaths={"supplier","createdBy","items","items.product","items.product.supplier"})
 java.util.Optional<PurchaseOrder> findById(Long id);
 @Override @EntityGraph(attributePaths={"supplier","createdBy","items","items.product","items.product.supplier"})
 java.util.List<PurchaseOrder> findAll();
}
