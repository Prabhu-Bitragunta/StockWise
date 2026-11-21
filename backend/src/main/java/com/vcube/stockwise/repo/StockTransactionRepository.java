package com.vcube.stockwise.repo;
import com.vcube.stockwise.domain.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
public interface StockTransactionRepository extends JpaRepository<StockTransaction,Long> {
 boolean existsByProduct_Id(Long productId);
 List<StockTransaction> findByProduct_IdAndTypeAndTransactionDateAfter(Long productId,String type,Instant since);
 List<StockTransaction> findByTransactionDateAfterOrderByTransactionDateAsc(Instant since);
 List<StockTransaction> findByTypeAndTransactionDateAfter(String type,Instant since);
}
