package in.jlenterprises.ecommerce.request.admin;

import jakarta.validation.constraints.Size;

public record SettingRequest(@Size(max = 20000) String value) {}
