package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.hr.EmploymentStatus;
import in.jlenterprises.ecommerce.dto.hr.EmployeeDto;
import in.jlenterprises.ecommerce.dto.hr.SalaryComponentDto;
import in.jlenterprises.ecommerce.entity.hr.Employee;
import in.jlenterprises.ecommerce.entity.hr.SalaryComponent;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.EmployeeRepository;
import in.jlenterprises.ecommerce.repository.SalaryComponentRepository;
import in.jlenterprises.ecommerce.request.hr.EmployeeRequest;
import in.jlenterprises.ecommerce.request.hr.SalaryComponentRequest;
import in.jlenterprises.ecommerce.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repository;
    private final SalaryComponentRepository componentRepository;

    public EmployeeServiceImpl(EmployeeRepository repository, SalaryComponentRepository componentRepository) {
        this.repository = repository;
        this.componentRepository = componentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDto> list(String q, Pageable pageable) {
        return repository.search(q == null ? null : q.trim(), pageable).map(e -> toDto(e, false));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto get(UUID id) {
        return toDto(entity(id), true);
    }

    @Override
    @Transactional
    public EmployeeDto create(EmployeeRequest r) {
        if (repository.existsByEmployeeCode(r.employeeCode().trim())) {
            throw new BusinessException("Employee code already exists.");
        }
        Employee e = new Employee();
        apply(e, r);
        return toDto(repository.save(e), true);
    }

    @Override
    @Transactional
    public EmployeeDto update(UUID id, EmployeeRequest r) {
        Employee e = entity(id);
        if (!e.getEmployeeCode().equalsIgnoreCase(r.employeeCode().trim())
                && repository.existsByEmployeeCode(r.employeeCode().trim())) {
            throw new BusinessException("Employee code already exists.");
        }
        apply(e, r);
        return toDto(repository.save(e), true);
    }

    @Override
    @Transactional
    public EmployeeDto setStatus(UUID id, EmploymentStatus status) {
        Employee e = entity(id);
        e.setEmploymentStatus(status);
        return toDto(repository.save(e), false);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Employee e = entity(id);
        e.setDeleted(true);
        repository.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryComponentDto> listComponents(UUID employeeId) {
        return componentRepository.findByEmployee_IdOrderByCreatedAtAsc(employeeId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public SalaryComponentDto addComponent(UUID employeeId, SalaryComponentRequest r) {
        Employee e = entity(employeeId);
        SalaryComponent c = new SalaryComponent();
        c.setEmployee(e);
        c.setComponentType(r.componentType());
        c.setLabel(r.label().trim());
        c.setAmount(r.amount());
        return toDto(componentRepository.save(c));
    }

    @Override
    @Transactional
    public void deleteComponent(UUID componentId) {
        SalaryComponent c = componentRepository.findById(componentId)
                .orElseThrow(() -> ResourceNotFoundException.of("SalaryComponent", componentId));
        c.setDeleted(true);
        componentRepository.save(c);
    }

    // ── helpers ──
    private Employee entity(UUID id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Employee", id));
    }

    private void apply(Employee e, EmployeeRequest r) {
        e.setEmployeeCode(r.employeeCode().trim());
        e.setFirstName(r.firstName().trim());
        e.setLastName(trimOrNull(r.lastName()));
        e.setPhone(trimOrNull(r.phone()));
        e.setEmail(trimOrNull(r.email()));
        e.setPhotoUrl(trimOrNull(r.photoUrl()));
        e.setDesignation(trimOrNull(r.designation()));
        e.setDepartment(trimOrNull(r.department()));
        e.setDateOfJoining(r.dateOfJoining());
        e.setEmploymentType(r.employmentType());
        if (r.employmentStatus() != null) e.setEmploymentStatus(r.employmentStatus());
        e.setBankAccountName(trimOrNull(r.bankAccountName()));
        e.setBankAccountNumber(trimOrNull(r.bankAccountNumber()));
        e.setIfsc(trimOrNull(r.ifsc()));
        e.setPan(trimOrNull(r.pan()));
        e.setAddress(trimOrNull(r.address()));
        e.setMonthlyBasic(nz(r.monthlyBasic()));
        e.setDailyRate(nz(r.dailyRate()));
        e.setPerServiceRate(nz(r.perServiceRate()));
        e.setHourlyRate(nz(r.hourlyRate()));
        e.setAnnualPaidLeave(r.annualPaidLeave() == null ? 12 : r.annualPaidLeave());
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private EmployeeDto toDto(Employee e, boolean withComponents) {
        List<SalaryComponentDto> comps = withComponents ? listComponents(e.getId()) : List.of();
        return new EmployeeDto(e.getId(), e.getEmployeeCode(), e.getFirstName(), e.getLastName(), e.getPhone(),
                e.getEmail(), e.getPhotoUrl(), e.getDesignation(), e.getDepartment(), e.getDateOfJoining(),
                e.getEmploymentType(), e.getEmploymentStatus(), e.getBankAccountName(), e.getBankAccountNumber(),
                e.getIfsc(), e.getPan(), e.getAddress(), e.getMonthlyBasic(), e.getDailyRate(), e.getPerServiceRate(),
                e.getHourlyRate(), e.getAnnualPaidLeave(), comps, e.getCreatedAt());
    }

    private SalaryComponentDto toDto(SalaryComponent c) {
        return new SalaryComponentDto(c.getId(), c.getComponentType(), c.getLabel(), c.getAmount());
    }
}
