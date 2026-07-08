package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.hr.EmploymentStatus;
import in.jlenterprises.ecommerce.entity.hr.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    boolean existsByEmployeeCode(String employeeCode);

    @Query("select e from Employee e where :q is null or :q = '' "
            + "or lower(e.firstName) like lower(concat('%', :q, '%')) "
            + "or lower(e.lastName) like lower(concat('%', :q, '%')) "
            + "or lower(e.employeeCode) like lower(concat('%', :q, '%')) "
            + "or lower(e.designation) like lower(concat('%', :q, '%')) "
            + "or e.phone like concat('%', :q, '%')")
    Page<Employee> search(@Param("q") String q, Pageable pageable);

    long countByEmploymentStatus(EmploymentStatus status);
}
