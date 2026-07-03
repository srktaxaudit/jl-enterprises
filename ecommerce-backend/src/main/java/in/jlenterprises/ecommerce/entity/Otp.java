package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.OtpPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * A one-time password record. The live/fast-path OTP check runs against Redis;
 * this table gives durable history and an at-rest fallback. Only the hash of
 * the code is stored — never the plain code.
 */
@Entity
@Table(name = "otps", indexes = {
        @Index(name = "idx_otp_identifier", columnList = "identifier"),
        @Index(name = "idx_otp_purpose", columnList = "purpose")
})
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Otp extends BaseEntity {

    /** Email or phone the OTP was sent to. */
    @Column(name = "identifier", nullable = false, length = 160)
    private String identifier;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "consumed", nullable = false)
    private boolean consumed = false;
}
