package com.vcube.stockwise.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="manager_role_requests", indexes={
    @Index(name="idx_manager_request_status_created", columnList="status,created_at"),
    @Index(name="idx_manager_request_user", columnList="user_id")
})
public class ManagerRoleRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private AppUser user;

    @Column(nullable=false, length=1000)
    private String reason;

    @Column(nullable=false, length=16)
    private String status="PENDING";

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt=Instant.now();

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="reviewed_by")
    private AppUser reviewedBy;

    @Column(name="reviewed_at")
    private Instant reviewedAt;

    protected ManagerRoleRequest() {}

    public ManagerRoleRequest(AppUser user,String reason) {
        this.user=user;
        this.reason=reason;
    }

    public Long getId(){return id;}
    public AppUser getUser(){return user;}
    public String getReason(){return reason;}
    public String getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;}
    public AppUser getReviewedBy(){return reviewedBy;}
    public Instant getReviewedAt(){return reviewedAt;}
    public void review(String status,AppUser reviewer){this.status=status;this.reviewedBy=reviewer;this.reviewedAt=Instant.now();}
}
