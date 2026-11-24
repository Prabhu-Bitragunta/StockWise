package com.vcube.stockwise.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="account_action_tokens", indexes=@Index(name="idx_account_action_expires", columnList="expires_at"))
public class AccountActionToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private AppUser user;

    @Column(name="token_hash", nullable=false, unique=true, length=64)
    private String tokenHash;

    @Column(nullable=false, length=24)
    private String purpose;

    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;

    @Column(name="created_at", nullable=false)
    private Instant createdAt=Instant.now();

    @Column(name="attempt_count", nullable=false)
    private int attemptCount=0;

    protected AccountActionToken() {}

    public AccountActionToken(AppUser user,String tokenHash,String purpose,Instant expiresAt) {
        this.user=user;
        this.tokenHash=tokenHash;
        this.purpose=purpose;
        this.expiresAt=expiresAt;
    }

    public Long getId(){return id;}
    public AppUser getUser(){return user;}
    public String getPurpose(){return purpose;}
    public String getTokenHash(){return tokenHash;}
    public Instant getExpiresAt(){return expiresAt;}
    public int getAttemptCount(){return attemptCount;}
    public void incrementAttemptCount(){attemptCount++;}
}
