package in.jlenterprises.ecommerce.controller.auth;

import in.jlenterprises.ecommerce.dto.auth.AuthResponse;
import in.jlenterprises.ecommerce.dto.auth.UserDto;
import in.jlenterprises.ecommerce.request.auth.ChangePasswordRequest;
import in.jlenterprises.ecommerce.request.auth.ForgotPasswordRequest;
import in.jlenterprises.ecommerce.request.auth.LoginRequest;
import in.jlenterprises.ecommerce.request.auth.RefreshTokenRequest;
import in.jlenterprises.ecommerce.request.auth.RegisterRequest;
import in.jlenterprises.ecommerce.request.auth.ResetPasswordRequest;
import in.jlenterprises.ecommerce.request.auth.SendOtpRequest;
import in.jlenterprises.ecommerce.request.auth.UpdateProfileRequest;
import in.jlenterprises.ecommerce.request.auth.VerifyOtpRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication & account API. Controllers stay thin — all logic is in
 * {@link AuthService}. Every response uses the standard {@link ApiResponse} envelope.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, tokens, password and profile")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest http) {
        AuthResponse res = authService.register(request, userAgent(http), clientIp(http));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", res));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive access + refresh tokens")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.success("Login successful", authService.login(request, userAgent(http), clientIp(http)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token (rotates the refresh token)")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return ApiResponse.success(authService.refresh(request.refreshToken(), userAgent(http), clientIp(http)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.message("Logged out");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send a password-reset link (always returns success)")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.message("If an account exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a password using a valid reset token")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.message("Password reset successful");
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify an email address from the emailed link")
    public ApiResponse<Void> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ApiResponse.message("Email verified");
    }

    @PostMapping("/send-otp")
    @Operation(summary = "Send a one-time password to an email or phone")
    public ApiResponse<Void> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request);
        return ApiResponse.message("OTP sent");
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify a one-time password")
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ApiResponse.message("OTP verified");
    }

    // ── Authenticated ───────────────────────────────────────────────────
    @GetMapping("/me")
    @Operation(summary = "Get the current user's profile")
    public ApiResponse<UserDto> me() {
        return ApiResponse.success(authService.getProfile(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current user's profile")
    public ApiResponse<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated", authService.updateProfile(SecurityUtils.currentUserId(), request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the current user's password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.currentUserId(), request);
        return ApiResponse.message("Password changed");
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete (deactivate) the current user's account")
    public ApiResponse<Void> deleteAccount() {
        authService.deleteAccount(SecurityUtils.currentUserId());
        return ApiResponse.message("Account deleted");
    }

    // ── helpers ─────────────────────────────────────────────────────────
    private static String userAgent(HttpServletRequest http) {
        return http.getHeader(HttpHeaders.USER_AGENT);
    }

    private static String clientIp(HttpServletRequest http) {
        return in.jlenterprises.ecommerce.util.ClientIp.from(http);
    }
}
