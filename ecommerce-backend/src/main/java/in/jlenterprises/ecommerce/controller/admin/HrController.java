package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.hr.EmploymentStatus;
import in.jlenterprises.ecommerce.dto.hr.AttendanceDto;
import in.jlenterprises.ecommerce.dto.hr.AttendanceSummaryDto;
import in.jlenterprises.ecommerce.dto.hr.EmployeeDto;
import in.jlenterprises.ecommerce.dto.hr.HrOverviewDto;
import in.jlenterprises.ecommerce.dto.hr.PayslipDto;
import in.jlenterprises.ecommerce.dto.hr.SalaryComponentDto;
import in.jlenterprises.ecommerce.request.hr.AttendanceMarkRequest;
import in.jlenterprises.ecommerce.request.hr.EmployeeRequest;
import in.jlenterprises.ecommerce.request.hr.PayslipGenerateRequest;
import in.jlenterprises.ecommerce.request.hr.SalaryComponentRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.AttendanceService;
import in.jlenterprises.ecommerce.service.EmployeeService;
import in.jlenterprises.ecommerce.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Human Resources — employees, attendance, payroll (Admin + HR only). */
@RestController
@RequestMapping("/api/v1/admin/hr")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR')")
@Tag(name = "Admin — HR", description = "Employees, attendance and payroll")
public class HrController {

    private final EmployeeService employees;
    private final AttendanceService attendance;
    private final PayrollService payroll;

    public HrController(EmployeeService employees, AttendanceService attendance, PayrollService payroll) {
        this.employees = employees;
        this.attendance = attendance;
        this.payroll = payroll;
    }

    // ── Employees ──
    @GetMapping("/employees")
    @Operation(summary = "List employees (paged, optional search)")
    public ApiResponse<PageResponse<EmployeeDto>> listEmployees(@RequestParam(required = false) String search,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(employees.list(search, pageable)));
    }

    @GetMapping("/employees/{id}")
    @Operation(summary = "Get an employee")
    public ApiResponse<EmployeeDto> getEmployee(@PathVariable UUID id) {
        return ApiResponse.success(employees.get(id));
    }

    @PostMapping("/employees")
    @Operation(summary = "Create an employee")
    public ResponseEntity<ApiResponse<EmployeeDto>> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Employee created", employees.create(request)));
    }

    @PutMapping("/employees/{id}")
    @Operation(summary = "Update an employee")
    public ApiResponse<EmployeeDto> updateEmployee(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success("Employee updated", employees.update(id, request));
    }

    @PatchMapping("/employees/{id}/status")
    @Operation(summary = "Set employment status")
    public ApiResponse<EmployeeDto> setStatus(@PathVariable UUID id, @RequestParam EmploymentStatus status) {
        return ApiResponse.success("Status updated", employees.setStatus(id, status));
    }

    @DeleteMapping("/employees/{id}")
    @Operation(summary = "Delete an employee")
    public ApiResponse<Void> deleteEmployee(@PathVariable UUID id) {
        employees.delete(id);
        return ApiResponse.message("Employee removed");
    }

    @GetMapping("/employees/{id}/components")
    @Operation(summary = "List an employee's salary components")
    public ApiResponse<List<SalaryComponentDto>> listComponents(@PathVariable UUID id) {
        return ApiResponse.success(employees.listComponents(id));
    }

    @PostMapping("/employees/{id}/components")
    @Operation(summary = "Add a salary component")
    public ApiResponse<SalaryComponentDto> addComponent(@PathVariable UUID id, @Valid @RequestBody SalaryComponentRequest request) {
        return ApiResponse.success("Component added", employees.addComponent(id, request));
    }

    @DeleteMapping("/components/{componentId}")
    @Operation(summary = "Delete a salary component")
    public ApiResponse<Void> deleteComponent(@PathVariable UUID componentId) {
        employees.deleteComponent(componentId);
        return ApiResponse.message("Component removed");
    }

    // ── Attendance ──
    @GetMapping("/attendance")
    @Operation(summary = "An employee's attendance for a month")
    public ApiResponse<List<AttendanceDto>> monthAttendance(@RequestParam UUID employeeId,
                                                            @RequestParam int year, @RequestParam int month) {
        return ApiResponse.success(attendance.month(employeeId, year, month));
    }

    @PostMapping("/attendance")
    @Operation(summary = "Mark (upsert) attendance for a day")
    public ApiResponse<AttendanceDto> markAttendance(@Valid @RequestBody AttendanceMarkRequest request) {
        return ApiResponse.success("Attendance saved", attendance.mark(request));
    }

    @GetMapping("/attendance/summary")
    @Operation(summary = "Monthly attendance summary + leave remaining")
    public ApiResponse<AttendanceSummaryDto> attendanceSummary(@RequestParam UUID employeeId,
                                                              @RequestParam int year, @RequestParam int month) {
        return ApiResponse.success(attendance.summary(employeeId, year, month));
    }

    // ── Payroll ──
    @PostMapping("/payslips")
    @Operation(summary = "Generate (or regenerate) a salary slip")
    public ApiResponse<PayslipDto> generatePayslip(@Valid @RequestBody PayslipGenerateRequest request) {
        return ApiResponse.success("Salary slip generated", payroll.generate(request));
    }

    @GetMapping("/payslips")
    @Operation(summary = "List salary slips (optional filters)")
    public ApiResponse<List<PayslipDto>> listPayslips(@RequestParam(required = false) UUID employeeId,
                                                      @RequestParam(required = false) Integer year,
                                                      @RequestParam(required = false) Integer month) {
        return ApiResponse.success(payroll.list(employeeId, year, month));
    }

    @GetMapping("/payslips/{id}")
    @Operation(summary = "Get a salary slip")
    public ApiResponse<PayslipDto> getPayslip(@PathVariable UUID id) {
        return ApiResponse.success(payroll.get(id));
    }

    @GetMapping("/payslips/{id}/pdf")
    @Operation(summary = "Download the salary-slip PDF")
    public ResponseEntity<byte[]> payslipPdf(@PathVariable UUID id) {
        PayslipDto p = payroll.get(id);
        byte[] pdf = payroll.pdf(id);
        String filename = "payslip-" + (p.employeeCode() == null ? "emp" : p.employeeCode())
                + "-" + p.periodYear() + "-" + String.format("%02d", p.periodMonth()) + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ── Overview ──
    @GetMapping("/overview")
    @Operation(summary = "HR overview stats")
    public ApiResponse<HrOverviewDto> overview() {
        return ApiResponse.success(payroll.overview());
    }
}
