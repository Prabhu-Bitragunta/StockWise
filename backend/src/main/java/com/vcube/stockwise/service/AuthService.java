package com.vcube.stockwise.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.vcube.stockwise.api.AuthRequests.*;
import com.vcube.stockwise.api.AuthRequests.AuthResponse;
import com.vcube.stockwise.api.AuthRequests.ChangePassword;
import com.vcube.stockwise.api.AuthRequests.EmailRequest;
import com.vcube.stockwise.api.AuthRequests.Login;
import com.vcube.stockwise.api.AuthRequests.ManagerRoleRequestView;
import com.vcube.stockwise.api.AuthRequests.MessageResponse;
import com.vcube.stockwise.api.AuthRequests.Register;
import com.vcube.stockwise.api.AuthRequests.ResetPassword;
import com.vcube.stockwise.api.AuthRequests.UserView;
import com.vcube.stockwise.api.AuthRequests.VerifyEmail;
import com.vcube.stockwise.api.AuthRequests.VerifyEmailOtp;
import com.vcube.stockwise.domain.AccountActionToken;
import com.vcube.stockwise.domain.AppUser;
import com.vcube.stockwise.domain.AuthSession;
import com.vcube.stockwise.domain.PendingRegistrationOtp;
import com.vcube.stockwise.repo.AccountActionTokenRepository;
import com.vcube.stockwise.repo.AppUserRepository;
import com.vcube.stockwise.repo.AuthSessionRepository;
import com.vcube.stockwise.repo.ManagerRoleRequestRepository;
import com.vcube.stockwise.repo.PendingRegistrationOtpRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class AuthService {
    private static final Logger LOG=LoggerFactory.getLogger(AuthService.class);
    private static final Set<String> ROLES=Set.of("ADMIN","MANAGER","STAFF");
    private static final String VERIFY_EMAIL="VERIFY_EMAIL";
    private static final String RESET_PASSWORD="RESET_PASSWORD";
    private static final SecureRandom RANDOM=new SecureRandom();
    @Value("${app.jwt.secret}") private String jwtSecret;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;
    @Value("${spring.mail.host:}") private String mailHost;
    @Value("${spring.mail.username:}") private String mailUsername;
    @Value("${app.mail.from:${spring.mail.username:}}") private String mailFrom;
    private SecretKey signingKey;
    private final AppUserRepository users; private final AuthSessionRepository sessions; private final PasswordEncoder passwords; private final ManagerRoleRequestRepository managerRequests; private final AccountActionTokenRepository actionTokens; private final JavaMailSender mailSender; private final PendingRegistrationOtpRepository pendingRegistrationOtps;
    public AuthService(AppUserRepository users,AuthSessionRepository sessions,PasswordEncoder passwords,ManagerRoleRequestRepository managerRequests,AccountActionTokenRepository actionTokens,PendingRegistrationOtpRepository pendingRegistrationOtps,JavaMailSender mailSender){this.users=users;this.sessions=sessions;this.passwords=passwords;this.managerRequests=managerRequests;this.actionTokens=actionTokens;this.pendingRegistrationOtps=pendingRegistrationOtps;this.mailSender=mailSender;}

    @PostConstruct void validateSecret(){if(jwtSecret==null||jwtSecret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");signingKey=Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));}

    @Transactional
    public MessageResponse requestRegistrationOtp(EmailRequest request){
        ensureMailConfigured();
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists. Sign in instead.");
        Instant now=Instant.now();
        Optional<PendingRegistrationOtp> existing=pendingRegistrationOtps.findByEmailIgnoreCase(email);
        if(existing.isPresent()&&existing.get().getExpiresAt().isAfter(now)&&existing.get().getCreatedAt().isAfter(now.minusSeconds(60)))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"A code was sent recently. Wait one minute before requesting another.");
        pendingRegistrationOtps.deleteByEmailIgnoreCase(email);
        String code=String.format(Locale.ROOT,"%06d",RANDOM.nextInt(1_000_000));
        pendingRegistrationOtps.save(new PendingRegistrationOtp(email,passwords.encode(code),now.plus(10,ChronoUnit.MINUTES)));
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(mailFrom);message.setTo(email);message.setSubject("Your Stockwise registration code");
        message.setText("Your Stockwise registration code is: "+code+"\n\nIt expires in 10 minutes and allows up to 5 attempts. If you did not request this, you can ignore this email.");
        try{mailSender.send(message);}catch(RuntimeException mailError){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"We could not send the code. Check the mail settings and try again.");}
        return new MessageResponse("If this email address exis, a verification code will arrive shortly. Enter it here to continue.");
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public MessageResponse validateRegistrationOtp(VerifyEmailOtp request){
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        PendingRegistrationOtp pending=pendingRegistrationOtps.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Request a new registration code and try again."));
        if(!pending.getExpiresAt().isAfter(Instant.now())){pendingRegistrationOtps.delete(pending);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code has expired. Request a new registration code.");}
        if(pending.getAttemptCount()>=5){pendingRegistrationOtps.delete(pending);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Too many attempts. Request a new registration code.");}
        if(!passwords.matches(request.code(),pending.getCodeHash())){
            pending.incrementAttemptCount();
            if(pending.getAttemptCount()>=5){pendingRegistrationOtps.delete(pending);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Too many incorrect attempts. Request a new registration code.");}
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is incorrect. Check it and try again.");
        }
        return new MessageResponse("Email verified. Create a password to finish setting up your account.");
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public UserView register(Register request){
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");
        PendingRegistrationOtp pending=pendingRegistrationOtps.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Request a verification code before creating your account."));
        if(!pending.getExpiresAt().isAfter(Instant.now())){pendingRegistrationOtps.delete(pending);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code has expired. Request a new verification code.");}
        if(pending.getAttemptCount()>=5){pendingRegistrationOtps.delete(pending);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Too many attempts. Request a new verification code.");}
        if(!passwords.matches(request.code(),pending.getCodeHash())){
            pending.incrementAttemptCount();
            if(pending.getAttemptCount()>=5){pendingRegistrationOtps.delete(pending);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Too many incorrect attempts. Request a new verification code.");}
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is incorrect. Check it and try again.");
        }
        AppUser user=new AppUser(email,request.displayName().trim(),passwords.encode(request.password()));
        user.setEmailVerified(true);
        user=users.save(user);
        pendingRegistrationOtps.delete(pending);
        return view(user);
    }
    @Transactional
    public AuthResponse login(Login request){
        AppUser user=users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect"));
        if(!passwords.matches(request.password(),user.getPasswordHash()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect");
        if(!user.isEmailVerified())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Verify your email address with the code we sent before signing in. You can request another code.");
        return issue(user);
    }
    @Transactional
    public MessageResponse verifyEmail(VerifyEmail request){
        AccountActionToken token=findActionToken(request.token(),VERIFY_EMAIL);
        AppUser user=token.getUser();
        user.setEmailVerified(true);
        actionTokens.deleteByUser_IdAndPurpose(user.getId(),VERIFY_EMAIL);
        return new MessageResponse("Email verified. You can now sign in.");
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public MessageResponse verifyEmailOtp(VerifyEmailOtp request){
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        AppUser user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is invalid or expired. Request a new verification code."));
        if(user.isEmailVerified())return new MessageResponse("Email is already verified. You can sign in.");
        AccountActionToken token=actionTokens.findByUser_IdAndPurposeAndExpiresAtAfter(user.getId(),VERIFY_EMAIL,Instant.now()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is invalid or expired. Request a new verification code."));
        byte[] submitted=hash(request.code()).getBytes(StandardCharsets.UTF_8);
        byte[] expected=token.getTokenHash().getBytes(StandardCharsets.UTF_8);
        if(!MessageDigest.isEqual(submitted,expected)){
            token.incrementAttemptCount();
            if(token.getAttemptCount()>=5){actionTokens.delete(token);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Too many incorrect attempts. Request a new verification code.");}
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is incorrect. Check it and try again.");
        }
        user.setEmailVerified(true);
        actionTokens.deleteByUser_IdAndPurpose(user.getId(),VERIFY_EMAIL);
        return new MessageResponse("Email verified. You can now sign in.");
    }
    @Transactional
    public MessageResponse resendVerification(EmailRequest request){
        ensureMailConfigured();
        users.findByEmailIgnoreCase(request.email().trim()).filter(user->!user.isEmailVerified()).ifPresent(this::sendVerificationCodeWithoutAccountDisclosure);
        return new MessageResponse("If the address belongs to an unverified account, a verification code has been sent.");
    }
    @Transactional
    public MessageResponse requestPasswordReset(EmailRequest request){
        ensureMailConfigured();
        users.findByEmailIgnoreCase(request.email().trim()).filter(AppUser::isEmailVerified).ifPresent(this::sendPasswordResetCodeWithoutAccountDisclosure);
        return new MessageResponse("If a verified account exists for that address, a password reset code has been sent.");
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public MessageResponse resetPassword(ResetPassword request){
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        AppUser user=users.findByEmailIgnoreCase(email).filter(AppUser::isEmailVerified).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is invalid or expired. Request a new password reset code."));
        AccountActionToken token=actionTokens.findByUser_IdAndPurposeAndExpiresAtAfter(user.getId(),RESET_PASSWORD,Instant.now()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is invalid or expired. Request a new password reset code."));
        if(token.getAttemptCount()>=5){actionTokens.delete(token);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Too many attempts. Request a new password reset code.");}
        if(!passwords.matches(request.code(),token.getTokenHash())){
            token.incrementAttemptCount();
            if(token.getAttemptCount()>=5){actionTokens.delete(token);throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Too many incorrect attempts. Request a new password reset code.");}
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is incorrect. Check it and try again.");
        }
        user.setPasswordHash(passwords.encode(request.newPassword()));
        actionTokens.deleteByUser_Id(user.getId());
        sessions.deleteByUser_Id(user.getId());
        return new MessageResponse("Password updated. You can now sign in with your new password.");
    }
    @Transactional
    public AuthResponse changePassword(AppUser principal,ChangePassword request){
        AppUser user=users.findById(principal.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Your account is no longer available. Sign in again."));
        if(!passwords.matches(request.currentPassword(),user.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The current password is incorrect.");
        if(passwords.matches(request.newPassword(),user.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a new password that differs from your current password.");
        user.setPasswordHash(passwords.encode(request.newPassword()));
        sessions.deleteByUser_Id(user.getId());
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
        if("ADMIN".equals(user.getRole())&&!"ADMIN".equals(normalized))throw new ResponseStatusException(HttpStatus.CONFLICT,"Admin accounts cannot be downgraded.");
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
    private AccountActionToken findActionToken(String rawToken,String purpose){
        if(rawToken==null||rawToken.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This link is invalid or expired. Request a new one.");
        return actionTokens.findByTokenHashAndPurposeAndExpiresAtAfter(hash(rawToken),purpose,Instant.now()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"This link is invalid or expired. Request a new one."));
    }
    private void createAndSendVerificationCode(AppUser user){
        ensureMailConfigured();
        actionTokens.deleteByUser_IdAndPurpose(user.getId(),VERIFY_EMAIL);
        String code=String.format(Locale.ROOT,"%06d",RANDOM.nextInt(1_000_000));
        actionTokens.save(new AccountActionToken(user,hash(code),VERIFY_EMAIL,Instant.now().plus(10,ChronoUnit.MINUTES)));
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(mailFrom);message.setTo(user.getEmail());message.setSubject("Your Stockwise verification code");
        message.setText("Your Stockwise email verification code is: "+code+"\n\nIt expires in 10 minutes. You have up to 5 attempts. If you did not request this, you can ignore this email.");
        try{mailSender.send(message);}catch(RuntimeException mailError){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"We could not send the verification code. Check the mail settings and try again.");}
    }
    private void sendVerificationCodeWithoutAccountDisclosure(AppUser user){
        try{createAndSendVerificationCode(user);}catch(ResponseStatusException emailError){actionTokens.deleteByUser_IdAndPurpose(user.getId(),VERIFY_EMAIL);LOG.warn("Could not send verification code",emailError);}
    }
    private void sendPasswordResetCodeWithoutAccountDisclosure(AppUser user){
        actionTokens.deleteByUser_IdAndPurpose(user.getId(),RESET_PASSWORD);
        String code=String.format(Locale.ROOT,"%06d",RANDOM.nextInt(1_000_000));
        actionTokens.save(new AccountActionToken(user,passwords.encode(code),RESET_PASSWORD,Instant.now().plus(10,ChronoUnit.MINUTES)));
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(mailFrom);message.setTo(user.getEmail());message.setSubject("Your Stockwise password reset code");
        message.setText("Your Stockwise password reset code is: "+code+"\n\nIt expires in 10 minutes and allows up to 5 attempts. If you did not request this, you can ignore this email.");
        try{mailSender.send(message);}catch(RuntimeException mailError){actionTokens.deleteByUser_IdAndPurpose(user.getId(),RESET_PASSWORD);LOG.warn("Could not send password reset code",mailError);}
    }
    private void ensureMailConfigured(){if(mailHost==null||mailHost.isBlank()||mailUsername==null||mailUsername.isBlank()||mailFrom==null||mailFrom.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email delivery is not configured. Ask the administrator to configure email settings.");}
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
