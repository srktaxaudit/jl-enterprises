package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {

    /** [accountId, sumDebit, sumCredit] for all postings up to (and including) a date. */
    @Query("select l.account.id, coalesce(sum(l.debit),0), coalesce(sum(l.credit),0) "
            + "from JournalLine l where l.journalEntry.entryDate <= :asOf group by l.account.id")
    List<Object[]> sumByAccountAsOf(@Param("asOf") LocalDate asOf);

    /** [accountId, sumDebit, sumCredit] for postings within a date range (P&amp;L movements). */
    @Query("select l.account.id, coalesce(sum(l.debit),0), coalesce(sum(l.credit),0) "
            + "from JournalLine l where l.journalEntry.entryDate between :from and :to group by l.account.id")
    List<Object[]> sumByAccountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Ordered posting lines for one account in a date range (ledger statement). */
    @Query("select l from JournalLine l join fetch l.journalEntry e "
            + "where l.account.id = :accountId and e.entryDate between :from and :to "
            + "order by e.entryDate, e.voucherNumber")
    List<JournalLine> statement(@Param("accountId") UUID accountId,
                                @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Net (debit − credit) movement for an account strictly before a date (opening balance). */
    @Query("select coalesce(sum(l.debit),0) - coalesce(sum(l.credit),0) "
            + "from JournalLine l where l.account.id = :accountId and l.journalEntry.entryDate < :before")
    BigDecimal netMovementBefore(@Param("accountId") UUID accountId, @Param("before") LocalDate before);
}
