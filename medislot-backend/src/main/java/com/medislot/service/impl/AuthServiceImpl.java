package com.medislot.service.impl;

import com.medislot.config.AppProperties;
import com.medislot.dto.*;
import com.medislot.entity.*;
import com.medislot.exception.ApiException;
import com.medislot.repository.*;
import com.medislot.security.JwtService;
import com.medislot.service.AuditService;
import com.medislot.service.AuthService;
import com.medislot.service.OtpSender;
import com.medislot.util.HashUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PatientRepository patients;
    private final VerificationCodeRepository codes;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final OtpSender otpSender;
    private final AuditService audit;
    private final AppProperties props;
    private final String dummyHash; // equalises timing for unknown emails

    public AuthServiceImpl(UserRepository users, PatientRepository patients,
                           VerificationCodeRepository codes, RefreshTokenRepository refreshTokens,
                           PasswordEncoder encoder, JwtService jwt, OtpSender otpSender,
                           AuditService audit, AppProperties props) {
        this.users = users;
        this.patients = patients;
        this.codes = codes;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.otpSender = otpSender;
        this.audit = audit;
        this.props = props;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    // ---------- register ----------

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest req, String ip) {
        String email = normalize(req.email());
        String mobile = req.mobile().trim();

        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                    "An account with this email already exists.");
        }
        if (users.existsByMobile(mobile)) {
            throw new ApiException(HttpStatus.CONFLICT, "MOBILE_ALREADY_REGISTERED",
                    "An account with this mobile number already exists.");
        }

        User user = new User();
        user.setEmail(email);
        user.setMobile(mobile);
        user.setFullName(req.fullName().trim());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(Role.PATIENT);
        try {
            user = users.save(user);
        } catch (DataIntegrityViolationException e) { // lost a race with another registration
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REGISTERED",
                    "An account with these details already exists.");
        }

        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patients.save(patient);

        issueCode(user);
        audit.log(user.getId(), email, "REGISTER", "USER", String.valueOf(user.getId()), null, ip);
        return new RegisterResponse(email, true);
    }

    // ---------- verification ----------

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public void verify(VerifyRequest req, String ip) {
        String email = normalize(req.email());
        User user = users.findByEmail(email).orElseThrow(this::invalidCode);
        if (user.isVerified()) {
            return; // idempotent
        }
        VerificationCode vc = codes.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(this::invalidCode);

        if (vc.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CODE_EXPIRED",
                    "This code has expired. Please request a new one.");
        }
        if (vc.getAttempts() >= props.auth().otpMaxAttempts()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                    "Too many incorrect attempts. Please request a new code.");
        }
        if (!encoder.matches(req.code(), vc.getCodeHash())) {
            vc.setAttempts(vc.getAttempts() + 1);
            audit.log(user.getId(), email, "VERIFY_FAILED", "USER", String.valueOf(user.getId()), null, ip);
            throw invalidCode();
        }

        vc.setUsed(true);
        user.setVerified(true);
        audit.log(user.getId(), email, "VERIFY_SUCCESS", "USER", String.valueOf(user.getId()), null, ip);
    }

    @Override
    @Transactional
    public void resendCode(ResendCodeRequest req) {
        users.findByEmail(normalize(req.email())).filter(u -> !u.isVerified()).ifPresent(user -> {
            Optional<VerificationCode> last =
                    codes.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId());
            if (last.isPresent() && last.get().getCreatedAt()
                    .plusSeconds(props.auth().otpResendCooldownSeconds()).isAfter(Instant.now())) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_COOLDOWN",
                        "Please wait a moment before requesting another code.");
            }
            issueCode(user);
        });
    }

    // ---------- login / tokens ----------

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest req, String ip) {
        String email = normalize(req.email());
        Instant now = Instant.now();
        User user = users.findByEmail(email).orElse(null);

        if (user == null) {
            encoder.matches(req.password(), dummyHash);
            audit.log(null, email, "LOGIN_FAILED", "USER", null, "unknown_email", ip);
            throw badCredentials();
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            audit.log(user.getId(), email, "LOGIN_BLOCKED", "USER", String.valueOf(user.getId()), "locked", ip);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "ACCOUNT_LOCKED",
                    "Too many failed attempts. Please try again later.");
        }
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= props.auth().maxFailedLogins()) {
                user.setLockedUntil(now.plus(props.auth().lockMinutes(), ChronoUnit.MINUTES));
                user.setFailedLoginAttempts(0);
                audit.log(user.getId(), email, "ACCOUNT_LOCKED", "USER", String.valueOf(user.getId()), null, ip);
            } else {
                user.setFailedLoginAttempts(attempts);
            }
            audit.log(user.getId(), email, "LOGIN_FAILED", "USER", String.valueOf(user.getId()),
                    "bad_password", ip);
            throw badCredentials();
        }
        // Password is correct from here on, so it is safe to reveal account state.
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account has been disabled.");
        }
        if (!user.isVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_VERIFIED",
                    "Please verify your account before logging in.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        audit.log(user.getId(), email, "LOGIN_SUCCESS", "USER", String.valueOf(user.getId()), null, ip);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(RefreshRequest req) {
        RefreshToken token = refreshTokens.findByTokenHash(HashUtil.sha256Hex(req.refreshToken()))
                .orElseThrow(this::invalidRefresh);

        if (token.isRevoked()) {
            // A rotated-out token was presented again: treat as theft and end all sessions.
            refreshTokens.revokeAllForUser(token.getUserId());
            audit.log(token.getUserId(), null, "REFRESH_REUSE_DETECTED", "USER",
                    String.valueOf(token.getUserId()), null, null);
            throw invalidRefresh();
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw invalidRefresh();
        }
        User user = users.findById(token.getUserId())
                .filter(u -> u.isEnabled() && u.isVerified())
                .orElseThrow(this::invalidRefresh);

        token.setRevoked(true); // rotation: each refresh token works once
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(RefreshRequest req) {
        refreshTokens.findByTokenHash(HashUtil.sha256Hex(req.refreshToken())).ifPresent(t -> {
            t.setRevoked(true);
            audit.log(t.getUserId(), null, "LOGOUT", "USER", String.valueOf(t.getUserId()), null, null);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId).map(this::toUserResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Authentication is required."));
    }

    // ---------- helpers ----------

    private void issueCode(User user) {
        codes.invalidateAll(user.getId());
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        VerificationCode vc = new VerificationCode();
        vc.setUserId(user.getId());
        vc.setCodeHash(encoder.encode(code));
        vc.setExpiresAt(Instant.now().plus(props.auth().otpExpiryMinutes(), ChronoUnit.MINUTES));
        codes.save(vc);
        otpSender.send(user, code);
    }

    private AuthResponse buildAuthResponse(User user) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(HashUtil.sha256Hex(refreshToken)); // only the hash is stored
        entity.setExpiresAt(Instant.now().plus(props.jwt().refreshTokenDays(), ChronoUnit.DAYS));
        refreshTokens.save(entity);

        return new AuthResponse(jwt.generateAccessToken(user), refreshToken, "Bearer",
                props.jwt().accessTokenMinutes() * 60, toUserResponse(user));
    }

    private UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getMobile(),
                u.getRole(), u.isVerified());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private ApiException badCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
    }

    private ApiException invalidCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CODE", "The verification code is not valid.");
    }

    private ApiException invalidRefresh() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "Your session has expired. Please log in again.");
    }
}