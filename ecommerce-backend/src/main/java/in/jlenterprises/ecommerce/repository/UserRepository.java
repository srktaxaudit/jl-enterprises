package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.AuthProvider;
import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    /** Look up an account by its (canonical) phone number — used for login by mobile. */
    Optional<User> findByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /** Recipients for a WhatsApp/SMS broadcast — users who have a phone number. */
    List<User> findByPhoneNotNull();

    /** Users holding any of the given roles (e.g. admins) — for internal admin alerts. */
    @Query("select distinct u from User u join u.roles r where r.name in :names")
    List<User> findByRoleNames(@Param("names") Collection<RoleName> names);
}
