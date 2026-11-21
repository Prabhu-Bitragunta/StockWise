package com.vcube.stockwise.api;

import jakarta.validation.constraints.*;

public final class AuthRequests {
    private AuthRequests() {}
    public record Register(@NotBlank @Size(max=80) String displayName,
                           @NotBlank @Email @Size(max=254) String email,
                           @NotBlank @Size(min=10,max=128) String password) {}
    public record Login(@NotBlank @Email String email,@NotBlank String password) {}
    public record ChangeRole(@NotBlank String role) {}
    public record ManagerRoleRequest(@NotBlank @Size(max=1000) String reason) {}
    public record ManagerRoleReview(@NotBlank String status) {}
    public record UserView(Long id,String email,String displayName,String role) {}
    public record ManagerRoleRequestView(Long id,Long userId,String email,String displayName,String reason,String status,java.time.Instant createdAt,java.time.Instant reviewedAt,String reviewedBy) {}
    public record AuthResponse(String token,UserView user,java.time.Instant expiresAt) {}
}
