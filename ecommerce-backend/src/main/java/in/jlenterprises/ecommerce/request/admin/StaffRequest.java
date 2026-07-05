package in.jlenterprises.ecommerce.request.admin;

import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Create/update payload for a staff account. Password required on create; blank on update = unchanged. */
public record StaffRequest(
        @NotBlank @Email @Size(max = 160) String email,
        String password,
        @NotBlank @Size(max = 80) @Pattern(regexp = ValidationPatterns.NAME, message = ValidationPatterns.MSG_NAME) String firstName,
        @Size(max = 80) @Pattern(regexp = ValidationPatterns.NAME_OPT, message = ValidationPatterns.MSG_NAME) String lastName,
        @Size(max = 20) @Pattern(regexp = ValidationPatterns.PHONE_OPT, message = ValidationPatterns.MSG_PHONE) String phone,
        @Size(max = 80) @Pattern(regexp = ValidationPatterns.TITLE_OPT, message = ValidationPatterns.MSG_TITLE) String department,
        @Size(max = 80) @Pattern(regexp = ValidationPatterns.TITLE_OPT, message = ValidationPatterns.MSG_TITLE) String designation,
        List<RoleName> roles
) {}
