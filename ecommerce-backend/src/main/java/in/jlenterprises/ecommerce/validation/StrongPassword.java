package in.jlenterprises.ecommerce.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Requires ≥8 chars with at least one lowercase, uppercase, digit and special character. */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface StrongPassword {
    String message() default "Password must be at least 8 characters and include upper, lower, digit and special characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
