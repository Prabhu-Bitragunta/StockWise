package com.vcube.stockwise.repo;

import com.vcube.stockwise.domain.PendingRegistrationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PendingRegistrationOtpRepository extends JpaRepository<PendingRegistrationOtp,Long> {
    Optional<PendingRegistrationOtp> findByEmailIgnoreCase(String email);
    void deleteByEmailIgnoreCase(String email);
}
