package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.EmiRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmiRequestRepository extends JpaRepository<EmiRequest, UUID> {

    Page<EmiRequest> findByEmiStatus(String emiStatus, Pageable pageable);

    /** New (unhandled) EMI requests — for the sidebar count badge. */
    long countByEmiStatus(String emiStatus);
}
