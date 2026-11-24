package com.vcube.stockwise.api;

import com.vcube.stockwise.api.AuthRequests.*;
import com.vcube.stockwise.domain.AppUser;
import com.vcube.stockwise.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/registration-otp")
    public MessageResponse requestRegistrationOtp(@Valid @RequestBody EmailRequest body) {
        return auth.requestRegistrationOtp(body);
    }

    @PostMapping("/registration-otp/verify")
    public MessageResponse validateRegistrationOtp(@Valid @RequestBody VerifyEmailOtp body) {
        return auth.validateRegistrationOtp(body);
    }

    @PostMapping("/register")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody Register body) {
        return auth.register(body);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody Login body) {
        return auth.login(body);
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmail body) {
        return auth.verifyEmail(body);
    }

    @PostMapping("/verify-email-otp")
    public MessageResponse verifyEmailOtp(@Valid @RequestBody VerifyEmailOtp body) {
        return auth.verifyEmailOtp(body);
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest body) {
        return auth.resendVerification(body);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest body) {
        return auth.requestPasswordReset(body);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPassword body) {
        return auth.resetPassword(body);
    }

    @PatchMapping("/password")
    public AuthResponse changePassword(@AuthenticationPrincipal AppUser user, @Valid @RequestBody ChangePassword body) {
        return auth.changePassword(user, body);
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal AppUser user) {
        return auth.current(user);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.logout(authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserView> users() {
        return auth.listUsers();
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserView changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRole body) {
        return auth.changeRole(id, body.role());
    }

    @PostMapping("/role-requests")
    public ManagerRoleRequestView requestManagerRole(@AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ManagerRoleRequest body) {
        return auth.requestManagerRole(user, body);
    }

    @GetMapping("/role-requests/me")
    public List<ManagerRoleRequestView> myManagerRoleRequests(@AuthenticationPrincipal AppUser user) {
        return auth.myManagerRoleRequests(user);
    }

    @GetMapping("/role-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ManagerRoleRequestView> managerRoleRequests() {
        return auth.managerRoleRequests();
    }

    @PatchMapping("/role-requests/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ManagerRoleRequestView reviewManagerRoleRequest(@PathVariable Long id,
            @AuthenticationPrincipal AppUser admin, @Valid @RequestBody ManagerRoleReview body) {
        return auth.reviewManagerRoleRequest(id, body.status(), admin);
    }
}
