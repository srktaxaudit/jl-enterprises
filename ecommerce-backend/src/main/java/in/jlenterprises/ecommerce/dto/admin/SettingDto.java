package in.jlenterprises.ecommerce.dto.admin;

import java.time.Instant;

public record SettingDto(String key, String value, Instant updatedAt) {}
