package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.ContactEnquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContactEnquiryRepository extends JpaRepository<ContactEnquiry, UUID> {

    Page<ContactEnquiry> findByEnquiryStatus(String enquiryStatus, Pageable pageable);

    /** New (unread) enquiries — for the sidebar count badge. */
    long countByEnquiryStatus(String enquiryStatus);
}
