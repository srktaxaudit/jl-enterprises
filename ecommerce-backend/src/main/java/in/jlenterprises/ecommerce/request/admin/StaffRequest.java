package in.jlenterprises.ecommerce.request.admin;

import in.jlenterprises.ecommerce.constant.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Create/update payload for a staff account. Password required on create; blank on update = unchanged. */
public record StaffRequest(
        @NotBlank @Email @Size(max = 160) String email,
        String password,
        @NotBlank @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Size(max = 20) String phone,
        @Size(max = 80) String department,
        @Size(max = 80) String designation,
        List<RoleName> roles
) {}
