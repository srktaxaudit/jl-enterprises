package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.hr.HrOverviewDto;
import in.jlenterprises.ecommerce.dto.hr.PayslipDto;
import in.jlenterprises.ecommerce.request.hr.PayslipGenerateRequest;

import java.util.List;
import java.util.UUID;

public interface PayrollService {
    PayslipDto generate(PayslipGenerateRequest request);
    List<PayslipDto> list(UUID employeeId, Integer year, Integer month);
    PayslipDto get(UUID id);
    /** Rendered salary-slip PDF bytes. */
    byte[] pdf(UUID payslipId);
    HrOverviewDto overview();
}
