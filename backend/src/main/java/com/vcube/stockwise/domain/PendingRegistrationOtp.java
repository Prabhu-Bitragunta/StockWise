package com.vcube.stockwise.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="pending_registration_otps", indexes=@Index(name="idx_pending_registration_otp_expiry", columnList="expires_at"))
public class PendingRegistrationOtp {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=254)
    private String email;

    @Column(name="code_hash", nullable=false, length=100)
    private String codeHash;

    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;

    @Column(name="created_at", nullable=false)
    private Instant createdAt=Instant.now();

    @Column(name="attempt_count", nullable=false, columnDefinition="integer not null default 0")
    private int attemptCount=0;

    protected PendingRegistrationOtp() {}

    public PendingRegistrationOtp(String email,String codeHash,Instant expiresAt) {
        this.email=email;this.codeHash=codeHash;this.expiresAt=expiresAt;
    }

    public String getCodeHash(){return codeHash;}
    public Instant getExpiresAt(){return expiresAt;}
    public Instant getCreatedAt(){return createdAt;}
    public int getAttemptCount(){return attemptCount;}
    public void incrementAttemptCount(){attemptCount++;}
}
