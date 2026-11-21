package com.vcube.stockwise.repo;
import com.vcube.stockwise.domain.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem,Long> { boolean existsByProduct_Id(Long productId); }
