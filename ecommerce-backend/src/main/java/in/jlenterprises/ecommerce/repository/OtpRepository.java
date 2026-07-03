package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.OtpPurpose;
import in.jlenterprises.ecommerce.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {

    Optional<Otp> findTopByIdentifierAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
            String identifier, OtpPurpose purpose);
}
