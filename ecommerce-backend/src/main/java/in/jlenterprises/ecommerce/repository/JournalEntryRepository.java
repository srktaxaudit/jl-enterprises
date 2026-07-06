package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.VoucherType;
import in.jlenterprises.ecommerce.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID>, JpaSpecificationExecutor<JournalEntry> {

    /** Idempotency guard for auto-posting from a source record (e.g. an order). */
    boolean existsByReferenceIdAndVoucherType(UUID referenceId, VoucherType voucherType);
}
