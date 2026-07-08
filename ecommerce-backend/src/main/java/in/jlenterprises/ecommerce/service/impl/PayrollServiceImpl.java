package in.jlenterprises.ecommerce.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.hr.AttendanceStatus;
import in.jlenterprises.ecommerce.constant.hr.EmploymentStatus;
import in.jlenterprises.ecommerce.constant.hr.SalaryComponentType;
import in.jlenterprises.ecommerce.dto.hr.HrOverviewDto;
import in.jlenterprises.ecommerce.dto.hr.PayLine;
import in.jlenterprises.ecommerce.dto.hr.PayslipDto;
import in.jlenterprises.ecommerce.entity.hr.AttendanceRecord;
import in.jlenterprises.ecommerce.entity.hr.Employee;
import in.jlenterprises.ecommerce.entity.hr.Payslip;
import in.jlenterprises.ecommerce.entity.hr.SalaryComponent;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.AttendanceRepository;
import in.jlenterprises.ecommerce.repository.EmployeeRepository;
import in.jlenterprises.ecommerce.repository.PayslipRepository;
import in.jlenterprises.ecommerce.repository.SalaryComponentRepository;
import in.jlenterprises.ecommerce.request.hr.PayslipGenerateRequest;
import in.jlenterprises.ecommerce.service.PayrollService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PayrollServiceImpl implements PayrollService {

    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final EmployeeRepository employeeRepo;
    private final SalaryComponentRepository componentRepo;
    private final AttendanceRepository attendanceRepo;
    private final PayslipRepository payslipRepo;
    private final PayslipPdfService pdfService;
    private final ObjectMapper mapper;

    public PayrollServiceImpl(EmployeeRepository employeeRepo, SalaryComponentRepository componentRepo,
                              AttendanceRepository attendanceRepo, PayslipRepository payslipRepo,
                              PayslipPdfService pdfService, ObjectMapper mapper) {
        this.employeeRepo = employeeRepo;
        this.componentRepo = componentRepo;
        this.attendanceRepo = attendanceRepo;
        this.payslipRepo = payslipRepo;
        this.pdfService = pdfService;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    @Auditable(action = "GENERATE_PAYSLIP", entity = "hr_payslip")
    public PayslipDto generate(PayslipGenerateRequest r) {
        Employee e = employeeRepo.findById(r.employeeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", r.employeeId()));
        int year = r.year(), month = r.month();
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        int daysInMonth = from.lengthOfMonth();
        List<AttendanceRecord> recs = attendanceRepo.findByEmployee_IdAndWorkDateBetweenOrderByWorkDateAsc(e.getId(), from, to);

        int present = 0, half = 0, absent = 0, paidLeave = 0, unpaidLeave = 0; long units = 0;
        for (AttendanceRecord a : recs) {
            switch (a.getAttendanceStatus()) {
                case PRESENT -> present++;
                case HALF_DAY -> half++;
                case ABSENT -> absent++;
                case PAID_LEAVE -> paidLeave++;
                case UNPAID_LEAVE -> unpaidLeave++;
                default -> { }
            }
            units += a.getServiceUnits();
        }
        BigDecimal workedDays = BigDecimal.valueOf(present).add(HALF.multiply(BigDecimal.valueOf(half)));
        int unpaidDays = unpaidLeave + absent;

        List<PayLine> lines = new ArrayList<>();
        BigDecimal daysPayable;
        switch (e.getEmploymentType()) {
            case DAILY_WAGE -> {
                BigDecimal payDays = workedDays.add(BigDecimal.valueOf(paidLeave));
                BigDecimal wages = e.getDailyRate().multiply(payDays).setScale(2, RoundingMode.HALF_UP);
                lines.add(new PayLine("Daily wages (" + strip(payDays) + " days @ Rs." + strip(e.getDailyRate()) + ")", "EARNING", wages));
                daysPayable = payDays;
            }
            case SERVICE_BASED -> {
                BigDecimal servicePay = e.getPerServiceRate().multiply(BigDecimal.valueOf(units)).setScale(2, RoundingMode.HALF_UP);
                lines.add(new PayLine("Service pay (" + units + " units @ Rs." + strip(e.getPerServiceRate()) + ")", "EARNING", servicePay));
                daysPayable = workedDays;
            }
            default -> { // FULL_TIME / PART_TIME — fixed monthly with loss-of-pay for unpaid days
                lines.add(new PayLine("Basic salary", "EARNING", e.getMonthlyBasic()));
                if (unpaidDays > 0) {
                    BigDecimal perDay = daysInMonth > 0
                            ? e.getMonthlyBasic().divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    BigDecimal lop = perDay.multiply(BigDecimal.valueOf(unpaidDays)).setScale(2, RoundingMode.HALF_UP);
                    lines.add(new PayLine("Loss of pay (" + unpaidDays + " days)", "DEDUCTION", lop));
                }
                daysPayable = BigDecimal.valueOf((long) daysInMonth - unpaidDays);
            }
        }

        // Recurring components
        for (SalaryComponent c : componentRepo.findByEmployee_IdOrderByCreatedAtAsc(e.getId())) {
            lines.add(new PayLine(c.getLabel(), c.getComponentType().name(), c.getAmount()));
        }
        // Incentive (manual for Phase 1)
        BigDecimal incentive = r.incentiveAmount() == null ? BigDecimal.ZERO : r.incentiveAmount();
        if (incentive.signum() > 0) lines.add(new PayLine("Incentive", "EARNING", incentive));

        BigDecimal gross = BigDecimal.ZERO, deductions = BigDecimal.ZERO;
        for (PayLine l : lines) {
            if (SalaryComponentType.DEDUCTION.name().equalsIgnoreCase(l.type())) deductions = deductions.add(l.amount());
            else gross = gross.add(l.amount());
        }
        BigDecimal net = gross.subtract(deductions);

        Payslip p = payslipRepo.findByEmployee_IdAndPeriodYearAndPeriodMonth(e.getId(), year, month)
                .orElseGet(() -> { Payslip x = new Payslip(); x.setEmployee(e); x.setPeriodYear(year); x.setPeriodMonth(month); return x; });
        p.setGeneratedAt(Instant.now());
        p.setDaysPresent(workedDays);
        p.setDaysPayable(daysPayable);
        p.setGrossEarnings(gross);
        p.setTotalDeductions(deductions);
        p.setIncentiveAmount(incentive);
        p.setNetPay(net);
        p.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
        try {
            p.setBreakdownJson(mapper.writeValueAsString(lines));
        } catch (Exception ex) {
            p.setBreakdownJson("[]");
        }
        return toDto(payslipRepo.save(p));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayslipDto> list(UUID employeeId, Integer year, Integer month) {
        List<Payslip> slips = employeeId != null
                ? payslipRepo.findByEmployee_IdOrderByPeriodYearDescPeriodMonthDesc(employeeId)
                : payslipRepo.findAllByOrderByGeneratedAtDesc(PageRequest.of(0, 100)).getContent();
        return slips.stream()
                .filter(p -> year == null || p.getPeriodYear() == year)
                .filter(p -> month == null || p.getPeriodMonth() == month)
                .map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipDto get(UUID id) {
        return toDto(entity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] pdf(UUID payslipId) {
        Payslip p = entity(payslipId);
        return pdfService.build(p, p.getEmployee());
    }

    @Override
    @Transactional(readOnly = true)
    public HrOverviewDto overview() {
        LocalDate today = LocalDate.now();
        long presentToday = attendanceRepo.countByWorkDateAndAttendanceStatus(today, AttendanceStatus.PRESENT);
        BigDecimal monthTotal = payslipRepo.sumNetPayForMonth(today.getYear(), today.getMonthValue());
        return new HrOverviewDto(
                employeeRepo.count(),
                employeeRepo.countByEmploymentStatus(EmploymentStatus.ACTIVE),
                employeeRepo.countByEmploymentStatus(EmploymentStatus.ON_LEAVE),
                presentToday,
                today.getYear(),
                today.getMonthValue(),
                monthTotal == null ? BigDecimal.ZERO : monthTotal);
    }

    // ── helpers ──
    private Payslip entity(UUID id) {
        return payslipRepo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Payslip", id));
    }

    private static String strip(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private PayslipDto toDto(Payslip p) {
        Employee e = p.getEmployee();
        String name = ((e.getFirstName() == null ? "" : e.getFirstName()) + " "
                + (e.getLastName() == null ? "" : e.getLastName())).trim();
        return new PayslipDto(p.getId(), e.getId(), name, e.getEmployeeCode(), p.getPeriodYear(), p.getPeriodMonth(),
                p.getGeneratedAt(), p.getDaysPresent(), p.getDaysPayable(), p.getGrossEarnings(), p.getTotalDeductions(),
                p.getIncentiveAmount(), p.getNetPay(), p.getBreakdownJson(), p.getNotes());
    }
}
