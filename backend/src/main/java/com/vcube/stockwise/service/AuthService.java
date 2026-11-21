package com.vcube.stockwise.service;

import com.vcube.stockwise.api.AuthRequests.*;
import com.vcube.stockwise.domain.*;
import com.vcube.stockwise.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuthService {
    private static final Set<String> ROLES=Set.of("ADMIN","MANAGER","STAFF");
    @Value("${app.jwt.secret}") private String jwtSecret;
    private SecretKey signingKey;
    private final AppUserRepository users; private final AuthSessionRepository sessions; private final PasswordEncoder passwords; private final ManagerRoleRequestRepository managerRequests;
    public AuthService(AppUserRepository users,AuthSessionRepository sessions,PasswordEncoder passwords,ManagerRoleRequestRepository managerRequests){this.users=users;this.sessions=sessions;this.passwords=passwords;this.managerRequests=managerRequests;}

    @PostConstruct void validateSecret(){if(jwtSecret==null||jwtSecret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");signingKey=Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));}

    @Transactional
    public UserView register(Register request){
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");
        AppUser user=users.save(new AppUser(email,request.displayName().trim(),passwords.encode(request.password())));
        return view(user);
    }
    @Transactional
    public AuthResponse login(Login request){
        AppUser user=users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect"));
        if(!passwords.matches(request.password(),user.getPasswordHash()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect");
        return issue(user);
    }
    @Transactional(readOnly=true)
    public UserView current(AppUser user){return view(user);}
    @Transactional
    public void logout(String rawToken){if(rawToken!=null&&!rawToken.isBlank())sessions.deleteByTokenHash(hash(rawToken));}
    @Transactional(readOnly=true)
    public List<UserView> listUsers(){return users.findAll().stream().map(AuthService::view).toList();}
    @Transactional
    public UserView changeRole(Long id,String role){
        String normalized=role==null?"":role.trim().toUpperCase(Locale.ROOT);
        if(!ROLES.contains(normalized))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Role must be ADMIN, MANAGER, or STAFF");
        AppUser user=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        user.setRole(normalized);return view(user);
    }
    @Transactional
    public ManagerRoleRequestView requestManagerRole(AppUser requester,com.vcube.stockwise.api.AuthRequests.ManagerRoleRequest body){
        if(!"STAFF".equals(requester.getRole()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Only staff accounts can request manager access");
        if(managerRequests.existsByUser_IdAndStatus(requester.getId(),"PENDING"))throw new ResponseStatusException(HttpStatus.CONFLICT,"You already have a manager access request awaiting review");
        return managerRequestView(managerRequests.save(new com.vcube.stockwise.domain.ManagerRoleRequest(requester,body.reason().trim())));
    }
    @Transactional(readOnly=true)
    public List<ManagerRoleRequestView> myManagerRoleRequests(AppUser requester){return managerRequests.findByUser_IdOrderByCreatedAtDesc(requester.getId()).stream().map(AuthService::managerRequestView).toList();}
    @Transactional(readOnly=true)
    public List<ManagerRoleRequestView> managerRoleRequests(){return managerRequests.findAllByOrderByCreatedAtDesc().stream().map(AuthService::managerRequestView).toList();}
    @Transactional
    public ManagerRoleRequestView reviewManagerRoleRequest(Long id,String status,AppUser reviewer){
        String normalized=status==null?"":status.trim().toUpperCase(Locale.ROOT);
        if(!Set.of("APPROVED","REJECTED").contains(normalized))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Status must be APPROVED or REJECTED");
        var request=managerRequests.findByIdAndStatus(id,"PENDING").orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"This request has already been reviewed or no longer exists"));
        AppUser managedReviewer=users.findById(reviewer.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Reviewer account no longer exists"));
        if("APPROVED".equals(normalized))request.getUser().setRole("MANAGER");
        request.review(normalized,managedReviewer);
        return managerRequestView(request);
    }
    @Transactional(readOnly=true)
    public Optional<AppUser> resolve(String rawToken){
        if(rawToken==null||rawToken.isBlank())return Optional.empty();
        try{
            var claims=Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(rawToken).getPayload();
            String subject=claims.getSubject();if(subject==null||subject.isBlank()||claims.getExpiration()==null||claims.getExpiration().toInstant().isBefore(Instant.now()))return Optional.empty();
            if(sessions.findFirstByTokenHashAndExpiresAtAfter(hash(rawToken),Instant.now()).isEmpty())return Optional.empty();
            return users.findByEmailIgnoreCase(subject);
        }catch(Exception invalid){return Optional.empty();}
    }
    private AuthResponse issue(AppUser user){
        sessions.deleteByExpiresAtBefore(Instant.now());
        Instant expires=Instant.now().plus(12,ChronoUnit.HOURS);
        String token=createJwt(user.getEmail(),Instant.now(),expires);
        sessions.save(new AuthSession(user,hash(token),expires));
        return new AuthResponse(token,view(user),expires);
    }
    private String createJwt(String subject,Instant issuedAt,Instant expiresAt){
        return Jwts.builder().subject(subject).issuedAt(java.util.Date.from(issuedAt)).expiration(java.util.Date.from(expiresAt)).signWith(signingKey).compact();
    }
    private static String hash(String token){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException("SHA-256 is unavailable",e);}
    }
    private static UserView view(AppUser user){return new UserView(user.getId(),user.getEmail(),user.getDisplayName(),user.getRole());}
    private static ManagerRoleRequestView managerRequestView(com.vcube.stockwise.domain.ManagerRoleRequest request){return new ManagerRoleRequestView(request.getId(),request.getUser().getId(),request.getUser().getEmail(),request.getUser().getDisplayName(),request.getReason(),request.getStatus(),request.getCreatedAt(),request.getReviewedAt(),request.getReviewedBy()==null?null:request.getReviewedBy().getDisplayName());}
}
