package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.hr.AttendanceStatus;
import in.jlenterprises.ecommerce.entity.hr.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, UUID> {

    Optional<AttendanceRecord> findByEmployee_IdAndWorkDate(UUID employeeId, LocalDate workDate);

    List<AttendanceRecord> findByEmployee_IdAndWorkDateBetweenOrderByWorkDateAsc(UUID employeeId, LocalDate from, LocalDate to);

    long countByWorkDateAndAttendanceStatus(LocalDate workDate, AttendanceStatus attendanceStatus);
}
