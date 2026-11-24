package com.vcube.stockwise.repo;

import com.vcube.stockwise.domain.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession,Long> {
    Optional<AuthSession> findFirstByTokenHashAndExpiresAtAfter(String tokenHash,Instant now);
    void deleteByTokenHash(String tokenHash);
    void deleteByExpiresAtBefore(Instant now);
    void deleteByUser_Id(Long userId);
}
