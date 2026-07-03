package in.jlenterprises.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a business rule is violated (e.g. out of stock, coupon expired). Maps to 422 by default. */
public class BusinessException extends ApiException {

    public BusinessException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(status, message);
    }
}
