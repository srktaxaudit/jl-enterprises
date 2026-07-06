package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.DocumentType;
import in.jlenterprises.ecommerce.entity.AccountingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountingDocumentRepository
        extends JpaRepository<AccountingDocument, UUID>, JpaSpecificationExecutor<AccountingDocument> {

    long countByDocumentType(DocumentType documentType);
}
