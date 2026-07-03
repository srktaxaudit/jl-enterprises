package in.jlenterprises.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Thrown on a uniqueness conflict (duplicate email, SKU, slug, ...). Maps to 409. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
