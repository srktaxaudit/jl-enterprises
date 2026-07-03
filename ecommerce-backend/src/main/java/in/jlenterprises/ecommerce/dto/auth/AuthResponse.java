package in.jlenterprises.ecommerce.dto.auth;

/** Returned on successful login/register/refresh. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,   // access-token lifetime in seconds
        UserDto user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserDto user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
