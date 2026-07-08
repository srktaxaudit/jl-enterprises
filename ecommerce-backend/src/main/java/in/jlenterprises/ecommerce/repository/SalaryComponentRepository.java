package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.hr.SalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, UUID> {
    List<SalaryComponent> findByEmployee_IdOrderByCreatedAtAsc(UUID employeeId);
}
