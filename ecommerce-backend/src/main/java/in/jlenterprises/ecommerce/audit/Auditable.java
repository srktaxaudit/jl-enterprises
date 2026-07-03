package in.jlenterprises.ecommerce.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose successful execution should be written to the
 * audit trail by {@link AuditAspect}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Action code, e.g. {@code CHANGE_ORDER_STATUS}. */
    String action();

    /** Entity/domain the action concerns, e.g. {@code order}. */
    String entity();
}
