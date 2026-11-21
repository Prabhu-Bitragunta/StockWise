package com.vcube.stockwise.repo;

import com.vcube.stockwise.domain.ManagerRoleRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ManagerRoleRequestRepository extends JpaRepository<ManagerRoleRequest,Long> {
    boolean existsByUser_IdAndStatus(Long userId,String status);
    List<ManagerRoleRequest> findAllByOrderByCreatedAtDesc();
    List<ManagerRoleRequest> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<ManagerRoleRequest> findByIdAndStatus(Long id,String status);
}
