package com.vcube.stockwise.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="auth_sessions", indexes=@Index(name="idx_auth_session_hash", columnList="token_hash", unique=true))
public class AuthSession {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private AppUser user;
    @Column(name="token_hash", nullable=false, unique=true, length=64) private String tokenHash;
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    @Column(name="created_at", nullable=false) private Instant createdAt=Instant.now();

    protected AuthSession() {}
    public AuthSession(AppUser user,String tokenHash,Instant expiresAt){this.user=user;this.tokenHash=tokenHash;this.expiresAt=expiresAt;}
    public Long getId(){return id;} public AppUser getUser(){return user;} public String getTokenHash(){return tokenHash;} public Instant getExpiresAt(){return expiresAt;}
}
