package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.StockAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, UUID> {

    Page<StockAlert> findByAlertStatus(String alertStatus, Pageable pageable);

    /** New (unhandled) alerts — for the sidebar count badge. */
    long countByAlertStatus(String alertStatus);
}
