package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID>, JpaSpecificationExecutor<LedgerAccount> {

    Optional<LedgerAccount> findByCode(String code);

    boolean existsByCode(String code);

    /** Party ledgers imported from Vyapar are coded "VYP-P-*" (for rollback). */
    List<LedgerAccount> findByCodeStartingWith(String prefix);

    List<LedgerAccount> findAllByOrderByCodeAsc();

    List<LedgerAccount> findByActiveTrueOrderByCodeAsc();
}
