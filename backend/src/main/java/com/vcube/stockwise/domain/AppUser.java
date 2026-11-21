package com.vcube.stockwise.domain;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
@Entity @Table(name="users",indexes=@Index(name="idx_users_role",columnList="role"))
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=254) private String username;
 @Column(nullable=false,unique=true) private String email;
 @Column(nullable=false) private String displayName;
 @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
 @Column(nullable=false,length=20) private String role="STAFF";
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
 protected AppUser(){}
 public AppUser(String email,String displayName,String passwordHash){this.username=email;this.email=email;this.displayName=displayName;this.passwordHash=passwordHash;}
 public Long getId(){return id;} public String getUsername(){return username;} public String getEmail(){return email;} public String getDisplayName(){return displayName;} public String getRole(){return role;} @JsonIgnore public String getPasswordHash(){return passwordHash;} public Instant getCreatedAt(){return createdAt;}
 public void setRole(String role){this.role=role;}
}
