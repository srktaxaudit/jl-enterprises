package in.jlenterprises.ecommerce.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void acceptsStrongPassword() {
        assertTrue(validator.isValid("Passw0rd!", null));
    }

    @Test
    void rejectsWeakPasswords() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("short1!", null));       // < 8
        assertFalse(validator.isValid("alllowercase1!", null)); // no uppercase
        assertFalse(validator.isValid("NoSpecial123", null));   // no special
        assertFalse(validator.isValid("NoDigits!!", null));     // no digit
    }
}
