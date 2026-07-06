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

    /**
     * Find accounts whose phone ends with the given 10 digits, ignoring how the stored
     * value is formatted (+91…, spaces, etc.). Native query (so it also strips non-digits);
     * returns a List so duplicate/legacy rows never blow up a single-result lookup.
     * Excludes soft-deleted rows explicitly (native queries bypass @SQLRestriction).
     */
    @Query(value = "select * from users u where u.phone is not null and u.deleted = false "
            + "and right(regexp_replace(u.phone, '[^0-9]', '', 'g'), 10) = :last10", nativeQuery = true)
    List<User> findByPhoneLast10(@Param("last10") String last10);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /** Recipients for a WhatsApp/SMS broadcast — users who have a phone number. */
    List<User> findByPhoneNotNull();

    /** Users holding any of the given roles (e.g. admins) — for internal admin alerts. */
    @Query("select distinct u from User u join u.roles r where r.name in :names")
    List<User> findByRoleNames(@Param("names") Collection<RoleName> names);
}
