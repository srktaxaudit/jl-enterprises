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

    /** Contacts imported from Vyapar are tagged providerId "VYP-P-*" (for rollback). */
    List<User> findByProviderIdStartingWith(String prefix);

    /** Recipients for a WhatsApp/SMS broadcast — users who have a phone number. */
    List<User> findByPhoneNotNull();

    // ── WhatsApp marketing audiences ──
    /** Opted-in customers with a phone number. */
    List<User> findByWhatsappOptInTrueAndPhoneNotNull();

    /** Opted-in customers whose phone is verified. */
    List<User> findByWhatsappOptInTrueAndPhoneVerifiedTrueAndPhoneNotNull();

    /** Opted-in customers who have placed at least one order. */
    @Query("select distinct o.user from Order o where o.user.phone is not null and o.user.whatsappOptIn = true")
    List<User> findOptedInWithOrders();

    /** Users holding any of the given roles (e.g. admins) — for internal admin alerts. */
    @Query("select distinct u from User u join u.roles r where r.name in :names")
    List<User> findByRoleNames(@Param("names") Collection<RoleName> names);

    /** Count of enabled users holding the given role (soft-deleted rows excluded by @SQLRestriction).
        Used to protect the last active Super Admin from being locked out. */
    @Query("select count(distinct u.id) from User u join u.roles r where u.enabled = true and r.name = :role")
    long countEnabledByRole(@Param("role") RoleName role);

    // ── Broadcast audience picker facets (Phase 3) ──
    /** Ids of users who have placed at least one order. */
    @Query("select distinct o.user.id from Order o")
    List<UUID> findUserIdsWithOrders();

    /** Ids of users who have bought at least one product in the given category. */
    @Query("select distinct o.user.id from Order o join o.items it where it.product.category.id = :categoryId")
    List<UUID> findUserIdsWhoBoughtCategory(@Param("categoryId") UUID categoryId);

    /** Ids of users with a saved address in the given city (case-insensitive). */
    @Query("select distinct a.user.id from Address a where lower(a.city) = lower(:city)")
    List<UUID> findUserIdsInCity(@Param("city") String city);

    /** Distinct non-blank saved cities, for the picker's city dropdown. */
    @Query("select distinct a.city from Address a where a.city is not null and a.city <> '' order by a.city")
    List<String> findDistinctCities();

    /** Phone numbers that submitted an EMI request (matched to users by last-10 digits). */
    @Query("select distinct e.phone from EmiRequest e where e.phone is not null")
    List<String> findEmiRequestPhones();

    /** [userId, city] pairs (first saved city per user) for the given users — labels the picker rows. */
    @Query("select a.user.id, min(a.city) from Address a where a.user.id in :ids group by a.user.id")
    List<Object[]> findCitiesForUsers(@Param("ids") Collection<UUID> ids);
}
