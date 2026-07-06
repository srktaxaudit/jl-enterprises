package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.DocumentType;
import in.jlenterprises.ecommerce.entity.AccountingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccountingDocumentRepository
        extends JpaRepository<AccountingDocument, UUID>, JpaSpecificationExecutor<AccountingDocument> {

    long countByDocumentType(DocumentType documentType);

    /** Posted documents of the given types in a date range, with line items eagerly loaded (for GST reports). */
    @Query("select distinct d from AccountingDocument d left join fetch d.lines "
            + "where d.documentStatus = in.jlenterprises.ecommerce.constant.DocumentStatus.POSTED "
            + "and d.documentType in :types and d.documentDate between :from and :to")
    List<AccountingDocument> findPostedWithLines(@Param("types") Collection<DocumentType> types,
                                                 @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Posted documents of the given types up to a date (for outstanding/aging). */
    @Query("select d from AccountingDocument d "
            + "where d.documentStatus = in.jlenterprises.ecommerce.constant.DocumentStatus.POSTED "
            + "and d.documentType in :types and d.documentDate <= :asOf")
    List<AccountingDocument> findPostedByTypesAsOf(@Param("types") Collection<DocumentType> types,
                                                   @Param("asOf") LocalDate asOf);

    /** Earliest posted sales invoice date for a party (for credit-days control). */
    @Query("select min(d.documentDate) from AccountingDocument d "
            + "where d.documentStatus = in.jlenterprises.ecommerce.constant.DocumentStatus.POSTED "
            + "and d.documentType = in.jlenterprises.ecommerce.constant.DocumentType.SALES_INVOICE "
            + "and d.party.id = :partyId")
    LocalDate oldestSalesInvoiceDate(@Param("partyId") UUID partyId);
}
