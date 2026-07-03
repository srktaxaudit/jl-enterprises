package in.jlenterprises.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Thrown for invalid/expired JWT, refresh, OTP or verification tokens. Maps to 401. */
public class InvalidTokenException extends ApiException {

    public InvalidTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
