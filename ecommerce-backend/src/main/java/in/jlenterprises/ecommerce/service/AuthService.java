package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.auth.AuthResponse;
import in.jlenterprises.ecommerce.dto.auth.UserDto;
import in.jlenterprises.ecommerce.request.auth.ChangePasswordRequest;
import in.jlenterprises.ecommerce.request.auth.LoginRequest;
import in.jlenterprises.ecommerce.request.auth.RegisterRequest;
import in.jlenterprises.ecommerce.request.auth.ResetPasswordRequest;
import in.jlenterprises.ecommerce.request.auth.SendOtpRequest;
import in.jlenterprises.ecommerce.request.auth.UpdateProfileRequest;
import in.jlenterprises.ecommerce.request.auth.VerifyOtpRequest;

import java.util.UUID;

/** Authentication and account-management business operations. */
public interface AuthService {

    AuthResponse register(RegisterRequest request, String userAgent, String ip);

    AuthResponse login(LoginRequest request, String userAgent, String ip);

    AuthResponse refresh(String refreshToken, String userAgent, String ip);

    void logout(String refreshToken);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    void verifyEmail(String token);

    void sendOtp(SendOtpRequest request);

    void verifyOtp(VerifyOtpRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    UserDto getProfile(UUID userId);

    UserDto updateProfile(UUID userId, UpdateProfileRequest request);

    void deleteAccount(UUID userId);
}
