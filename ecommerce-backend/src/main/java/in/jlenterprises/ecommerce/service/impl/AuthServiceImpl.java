package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.config.AppProperties;
import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.constant.OtpPurpose;
import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.dto.auth.AuthResponse;
import in.jlenterprises.ecommerce.dto.auth.UserDto;
import in.jlenterprises.ecommerce.entity.RefreshToken;
import in.jlenterprises.ecommerce.entity.Role;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.ApiException;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.DuplicateResourceException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.UserMapper;
import in.jlenterprises.ecommerce.repository.RoleRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.request.auth.ChangePasswordRequest;
import in.jlenterprises.ecommerce.request.auth.LoginRequest;
import in.jlenterprises.ecommerce.request.auth.RegisterRequest;
import in.jlenterprises.ecommerce.request.auth.ResetPasswordRequest;
import in.jlenterprises.ecommerce.request.auth.SendOtpRequest;
import in.jlenterprises.ecommerce.request.auth.UpdateProfileRequest;
import in.jlenterprises.ecommerce.request.auth.VerifyOtpRequest;
import in.jlenterprises.ecommerce.security.LoginAttemptService;
import in.jlenterprises.ecommerce.security.jwt.JwtService;
import in.jlenterprises.ecommerce.service.AuthService;
import in.jlenterprises.ecommerce.service.NotificationService;
import in.jlenterprises.ecommerce.service.OtpService;
import in.jlenterprises.ecommerce.service.RefreshTokenService;
import in.jlenterprises.ecommerce.service.VerificationTokenService;
import in.jlenterprises.ecommerce.notification.EmailService;
import in.jlenterprises.ecommerce.util.IdentifierUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core authentication logic. Enforces per-account brute-force lockout (in
 * addition to the IP throttle), rotates refresh tokens, and never reveals
 * whether an email exists on the forgot-password path.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String PURPOSE_EMAIL_VERIFY = "email-verify";
    private static final String PURPOSE_RESET = "password-reset";
    private static final Duration EMAIL_VERIFY_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;
    private final UserMapper userMapper;
    private final AppProperties props;
    private final NotificationService notificationService;

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                           JwtService jwtService, RefreshTokenService refreshTokenService,
                           OtpService otpService, VerificationTokenService verificationTokenService,
                           EmailService emailService, LoginAttemptService loginAttemptService,
                           UserMapper userMapper, AppProperties props,
                           NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.otpService = otpService;
        this.verificationTokenService = verificationTokenService;
        this.emailService = emailService;
        this.loginAttemptService = loginAttemptService;
        this.userMapper = userMapper;
        this.props = props;
        this.notificationService = notificationService;
    }

    // ── Registration ───────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String userAgent, String ip) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        // Canonicalise to +91XXXXXXXXXX before storing, and check duplicates by last-10
        // digits so "+919876543210" and "9876543210" can never coexist as two accounts
        // (exact-string existsByPhone missed differently-formatted duplicates).
        String phone = null;
        if (request.phone() != null && !request.phone().isBlank()) {
            phone = IdentifierUtil.normalizePhone(request.phone());
            if (!userRepository.findByPhoneLast10(IdentifierUtil.last10(phone)).isEmpty()) {
                throw new DuplicateResourceException("An account with this phone already exists");
            }
        }

        Role customer = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role not configured"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(phone);
        if (Boolean.TRUE.equals(request.whatsappOptIn())) {
            user.setWhatsappOptIn(true);
            user.setWhatsappOptInAt(Instant.now());
        }
        user.getRoles().add(customer);
        user = userRepository.save(user);

        // Let admins know a new customer signed up (best-effort — never blocks signup).
        String newName = user.getFullName();
        notificationService.notifyAdmins(NotificationType.ACCOUNT, "New customer registered",
                "New customer registered: " + (newName == null || newName.isBlank() ? user.getEmail() : newName) + ".",
                "/admin-customers.html", "Customers", user.getId(), "USER");

        // Fire off an email-verification link — best-effort. This uses Redis
        // (token store) + SMTP, which are optional; if either is unavailable,
        // registration must still succeed (email verification is not required
        // to log in or shop).
        try {
            String token = verificationTokenService.issue(PURPOSE_EMAIL_VERIFY, email, EMAIL_VERIFY_TTL);
            emailService.sendVerificationLink(email, props.mail().verificationBaseUrl() + "?token=" + token);
        } catch (Exception e) {
            log.warn("Skipped email verification for {} (token store/mail unavailable): {}", email, e.getMessage());
        }

        return buildAuthResponse(user, false, userAgent, ip);
    }

    // ── Login (with lockout) ────────────────────────────────────────────
    // noRollbackFor is LOAD-BEARING: on a bad password we increment the failed-attempt
    // counter and then throw ApiException. A default rollback would silently discard that
    // increment — the account lockout could never engage. With ApiException exempted, the
    // counter (and the lock, once the threshold is reached) commits despite the throw.
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest request, String userAgent, String ip) {
        // The identifier may be an email or a mobile number. Normalise it to the same
        // canonical form used for storage/lookup so both paths authenticate uniformly.
        String raw = request.email().trim();
        String identifier = IdentifierUtil.isEmail(raw) ? raw.toLowerCase() : IdentifierUtil.normalizePhone(raw);
        String rateKey = ip + "|" + identifier;

        if (loginAttemptService.isBlocked(rateKey)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please try again later.");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, request.password()));
        } catch (LockedException e) {
            throw new ApiException(HttpStatus.LOCKED, "Account is temporarily locked. Please try again later.");
        } catch (AuthenticationException e) {
            // Covers bad password, unknown email/mobile, and any lookup failure —
            // all surface to the client as a single, non-revealing message.
            loginAttemptService.recordFailure(rateKey);
            registerFailedAttempt(identifier);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email/mobile or password");
        }

        User user = findByIdentifier(identifier)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email/mobile or password"));

        // Success — clear counters, stamp login.
        loginAttemptService.reset(rateKey);
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user, request.rememberMe(), userAgent, ip);
    }

    private void registerFailedAttempt(String identifier) {
        findByIdentifier(identifier).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= props.security().login().maxFailedAttempts()) {
                user.setAccountLocked(true);
                user.setLockedUntil(Instant.now().plus(props.security().login().lockDuration()));
                log.warn("Account locked due to failed logins: {}", identifier);
            }
            userRepository.save(user);
        });
    }

    /** Resolve an identifier (email or phone) to its account, matching a phone by its
        last 10 digits regardless of stored format. */
    private java.util.Optional<User> findByIdentifier(String identifier) {
        return IdentifierUtil.isEmail(identifier)
                ? userRepository.findByEmailIgnoreCase(identifier)
                : userRepository.findByPhoneLast10(IdentifierUtil.last10(identifier)).stream().findFirst();
    }

    // ── Refresh / logout ────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken, String userAgent, String ip) {
        RefreshToken current = refreshTokenService.verify(refreshToken);
        User user = current.getUser();
        // A disabled account must not keep a session alive by rotating refresh tokens
        // forever (the JWT filter blocks its ACCESS tokens, but refresh worked before).
        if (!user.isEnabled()) {
            refreshTokenService.revokeAll(user);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "This account has been disabled.");
        }
        RefreshToken rotated = refreshTokenService.rotate(current, userAgent, ip);
        String access = jwtService.generateAccessToken(user.getEmail(), roleNames(user));
        return AuthResponse.of(access, rotated.getRawToken(), jwtService.getAccessTtlSeconds(), userMapper.toDto(user));
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    // ── Password reset / verification ───────────────────────────────────
    @Override
    @Transactional
    public void forgotPassword(String email) {
        String normalized = email.trim().toLowerCase();
        // Always behave identically whether or not the account exists (no enumeration).
        userRepository.findByEmailIgnoreCase(normalized).ifPresent(user -> {
            String token = verificationTokenService.issue(PURPOSE_RESET, normalized, RESET_TTL);
            String sep = props.mail().passwordResetBaseUrl().contains("?") ? "&" : "?";
            String link = props.mail().passwordResetBaseUrl() + sep + "token=" + token;
            emailService.sendPasswordResetLink(normalized, link);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = verificationTokenService.consume(PURPOSE_RESET, request.token());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        refreshTokenService.revokeAll(user);   // force re-login everywhere
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        String email = verificationTokenService.consume(PURPOSE_EMAIL_VERIFY, token);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    // ── OTP ─────────────────────────────────────────────────────────────
    @Override
    public void sendOtp(SendOtpRequest request) {
        String code = otpService.generate(request.identifier(), request.purpose());
        if (request.identifier().contains("@")) {
            emailService.sendOtp(request.identifier(), code);
        } else {
            // SMS provider not configured yet — integration point for phone OTP.
            log.info("OTP for {} ({}) generated; SMS delivery not configured", request.identifier(), request.purpose());
        }
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        otpService.verify(request.identifier(), request.purpose(), request.code());
        if (request.purpose() == OtpPurpose.EMAIL_VERIFICATION && request.identifier().contains("@")) {
            userRepository.findByEmailIgnoreCase(request.identifier())
                    .ifPresent(u -> { u.setEmailVerified(true); userRepository.save(u); });
        } else if (request.purpose() == OtpPurpose.PHONE_VERIFICATION) {
            // The identifier is a phone number, not an email — match it against stored
            // phones by their last 10 digits (format-agnostic). The previous email lookup
            // always missed, so phoneVerified was never actually set.
            String digits = request.identifier().replaceAll("\\D", "");
            String last10 = digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
            java.util.List<User> matches = userRepository.findByPhoneLast10(last10);
            // Registration/profile updates now enforce phone uniqueness (last-10), so more
            // than one match means legacy duplicate rows — flag them for cleanup rather
            // than silently blessing every account with the number.
            if (matches.size() > 1) {
                log.warn("OTP phone-verify matched {} accounts for the same number — legacy duplicates "
                        + "need manual cleanup (last10 ending {}).", matches.size(), last10.substring(6));
            }
            matches.forEach(u -> u.setPhoneVerified(true));
            if (!matches.isEmpty()) userRepository.saveAll(matches);
        }
    }

    // ── Account management ──────────────────────────────────────────────
    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        return userMapper.toDto(getUser(userId));
    }

    @Override
    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.phone() != null && !request.phone().isBlank()) {
            // Same canonical form + last-10 duplicate check as registration (excluding self).
            String phone = IdentifierUtil.normalizePhone(request.phone());
            boolean taken = userRepository.findByPhoneLast10(IdentifierUtil.last10(phone)).stream()
                    .anyMatch(u -> !u.getId().equals(userId));
            if (taken) throw new DuplicateResourceException("Phone already in use");
            user.setPhone(phone);
        }
        if (request.whatsappOptIn() != null) {
            boolean opt = request.whatsappOptIn();
            user.setWhatsappOptIn(opt);
            user.setWhatsappOptInAt(opt ? Instant.now() : null);
        }
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = getUser(userId);
        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
    }

    // ── helpers ─────────────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user, boolean rememberMe, String userAgent, String ip) {
        String access = jwtService.generateAccessToken(user.getEmail(), roleNames(user));
        RefreshToken refresh = refreshTokenService.issue(user, rememberMe, userAgent, ip);
        return AuthResponse.of(access, refresh.getRawToken(), jwtService.getAccessTtlSeconds(), userMapper.toDto(user));
    }

    private List<String> roleNames(User user) {
        return user.getRoles().stream().map(r -> r.getName().name()).toList();
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
