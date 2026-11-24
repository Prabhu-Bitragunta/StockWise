package com.vcube.stockwise.api;

import jakarta.validation.constraints.*;

public final class AuthRequests {
    private AuthRequests() {}
    public record Register(@NotBlank @Size(max=80) String displayName,
                           @NotBlank @Email @Size(max=254) String email,
                           @NotBlank @Size(min=9,max=128,message="Password must be at least 9 characters and no more than 128 characters")
                           @Pattern(regexp="(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]).*",message="Password must include an uppercase letter, a lowercase letter, and a symbol") String password,
                           @NotBlank @Pattern(regexp="[0-9]{6}",message="Enter the 6-digit code from your email") String code) {}
    public record Login(@NotBlank @Email String email,@NotBlank String password) {}
    public record ChangePassword(@NotBlank String currentPassword,
                                 @NotBlank @Size(min=9,max=128,message="Password must be at least 9 characters and no more than 128 characters")
                                 @Pattern(regexp="(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]).*",message="Password must include an uppercase letter, a lowercase letter, and a symbol") String newPassword) {}
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) {}
    public record VerifyEmail(@NotBlank String token) {}
    public record VerifyEmailOtp(@NotBlank @Email @Size(max=254) String email,
                                 @NotBlank @Pattern(regexp="[0-9]{6}",message="Enter the 6-digit code from your email") String code) {}
    public record ResetPassword(@NotBlank @Email @Size(max=254) String email,
                                @NotBlank @Pattern(regexp="[0-9]{6}",message="Enter the 6-digit code from your email") String code,
                                @NotBlank @Size(min=9,max=128,message="Password must be at least 9 characters and no more than 128 characters")
                                @Pattern(regexp="(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]).*",message="Password must include an uppercase letter, a lowercase letter, and a symbol") String newPassword) {}
    public record MessageResponse(String message) {}
    public record ChangeRole(@NotBlank String role) {}
    public record ManagerRoleRequest(@NotBlank @Size(max=1000) String reason) {}
    public record ManagerRoleReview(@NotBlank String status) {}
    public record UserView(Long id,String email,String displayName,String role) {}
    public record ManagerRoleRequestView(Long id,Long userId,String email,String displayName,String reason,String status,java.time.Instant createdAt,java.time.Instant reviewedAt,String reviewedBy) {}
    public record AuthResponse(String token,UserView user,java.time.Instant expiresAt) {}
}
