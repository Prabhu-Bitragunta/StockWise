package com.vcube.stockwise.repo;

import com.vcube.stockwise.domain.AccountActionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken,Long> {
    Optional<AccountActionToken> findByTokenHashAndPurposeAndExpiresAtAfter(String tokenHash,String purpose,Instant now);
    Optional<AccountActionToken> findByUser_IdAndPurposeAndExpiresAtAfter(Long userId,String purpose,Instant now);
    void deleteByUser_IdAndPurpose(Long userId,String purpose);
    void deleteByUser_Id(Long userId);
}
