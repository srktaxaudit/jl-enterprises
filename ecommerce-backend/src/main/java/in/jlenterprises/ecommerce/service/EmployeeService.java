package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.hr.EmploymentStatus;
import in.jlenterprises.ecommerce.dto.hr.EmployeeDto;
import in.jlenterprises.ecommerce.dto.hr.SalaryComponentDto;
import in.jlenterprises.ecommerce.request.hr.EmployeeRequest;
import in.jlenterprises.ecommerce.request.hr.SalaryComponentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {
    Page<EmployeeDto> list(String q, Pageable pageable);
    EmployeeDto get(UUID id);
    EmployeeDto create(EmployeeRequest request);
    EmployeeDto update(UUID id, EmployeeRequest request);
    EmployeeDto setStatus(UUID id, EmploymentStatus status);
    void delete(UUID id);
    List<SalaryComponentDto> listComponents(UUID employeeId);
    SalaryComponentDto addComponent(UUID employeeId, SalaryComponentRequest request);
    void deleteComponent(UUID componentId);
}
