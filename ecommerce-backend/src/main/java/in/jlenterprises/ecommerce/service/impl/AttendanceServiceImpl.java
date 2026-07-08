package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.hr.AttendanceStatus;
import in.jlenterprises.ecommerce.dto.hr.AttendanceDto;
import in.jlenterprises.ecommerce.dto.hr.AttendanceSummaryDto;
import in.jlenterprises.ecommerce.entity.hr.AttendanceRecord;
import in.jlenterprises.ecommerce.entity.hr.Employee;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.AttendanceRepository;
import in.jlenterprises.ecommerce.repository.EmployeeRepository;
import in.jlenterprises.ecommerce.request.hr.AttendanceMarkRequest;
import in.jlenterprises.ecommerce.service.AttendanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceServiceImpl(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public AttendanceDto mark(AttendanceMarkRequest r) {
        Employee e = employeeRepository.findById(r.employeeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", r.employeeId()));
        AttendanceRecord rec = attendanceRepository.findByEmployee_IdAndWorkDate(e.getId(), r.workDate())
                .orElseGet(() -> {
                    AttendanceRecord a = new AttendanceRecord();
                    a.setEmployee(e);
                    a.setWorkDate(r.workDate());
                    return a;
                });
        rec.setAttendanceStatus(r.attendanceStatus());
        rec.setHoursWorked(r.hoursWorked());
        rec.setServiceUnits(r.serviceUnits() == null ? 0 : r.serviceUnits());
        rec.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        return toDto(attendanceRepository.save(rec));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> month(UUID employeeId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return attendanceRepository.findByEmployee_IdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, from, to)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryDto summary(UUID employeeId, int year, int month) {
        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<AttendanceRecord> recs = attendanceRepository.findByEmployee_IdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, from, to);

        int present = 0, half = 0, absent = 0, paidLeave = 0, unpaidLeave = 0, weekOff = 0, holiday = 0, units = 0;
        for (AttendanceRecord a : recs) {
            switch (a.getAttendanceStatus()) {
                case PRESENT -> present++;
                case HALF_DAY -> half++;
                case ABSENT -> absent++;
                case PAID_LEAVE -> paidLeave++;
                case UNPAID_LEAVE -> unpaidLeave++;
                case WEEK_OFF -> weekOff++;
                case HOLIDAY -> holiday++;
            }
            units += a.getServiceUnits();
        }
        BigDecimal payableDays = BigDecimal.valueOf(present)
                .add(BigDecimal.valueOf(half).multiply(new BigDecimal("0.5")))
                .add(BigDecimal.valueOf(paidLeave));

        // Leave remaining = annual entitlement minus paid leaves taken across the whole year.
        LocalDate yearFrom = LocalDate.of(year, 1, 1);
        LocalDate yearTo = LocalDate.of(year, 12, 31);
        long yearPaidLeaves = attendanceRepository
                .findByEmployee_IdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, yearFrom, yearTo)
                .stream().filter(a -> a.getAttendanceStatus() == AttendanceStatus.PAID_LEAVE).count();
        int leaveRemaining = e.getAnnualPaidLeave() - (int) yearPaidLeaves;

        return new AttendanceSummaryDto(year, month, present, half, absent, paidLeave, unpaidLeave, weekOff, holiday,
                units, payableDays, leaveRemaining);
    }

    private AttendanceDto toDto(AttendanceRecord a) {
        return new AttendanceDto(a.getId(), a.getWorkDate(), a.getAttendanceStatus(), a.getHoursWorked(),
                a.getServiceUnits(), a.getNote());
    }
}
