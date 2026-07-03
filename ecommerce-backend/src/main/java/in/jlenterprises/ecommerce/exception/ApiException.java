package in.jlenterprises.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Base for all deliberate, client-facing application errors. Carries the HTTP status to return. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
