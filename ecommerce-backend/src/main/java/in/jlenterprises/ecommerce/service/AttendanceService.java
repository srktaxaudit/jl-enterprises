package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.hr.AttendanceDto;
import in.jlenterprises.ecommerce.dto.hr.AttendanceSummaryDto;
import in.jlenterprises.ecommerce.request.hr.AttendanceMarkRequest;

import java.util.List;
import java.util.UUID;

public interface AttendanceService {
    AttendanceDto mark(AttendanceMarkRequest request);
    List<AttendanceDto> month(UUID employeeId, int year, int month);
    AttendanceSummaryDto summary(UUID employeeId, int year, int month);
}
