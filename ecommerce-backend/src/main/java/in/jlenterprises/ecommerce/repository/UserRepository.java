package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.AuthProvider;
import in.jlenterprises.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /** Recipients for a WhatsApp/SMS broadcast — users who have a phone number. */
    List<User> findByPhoneNotNull();
}
