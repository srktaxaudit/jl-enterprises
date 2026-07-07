package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.ExchangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, UUID> {

    List<ExchangeRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<ExchangeRequest> findByUserId(UUID userId, Pageable pageable);
}
