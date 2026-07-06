package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.accounting.AgingReportDto;
import in.jlenterprises.ecommerce.dto.accounting.CashFlowDto;
import in.jlenterprises.ecommerce.dto.accounting.EwayBillDto;
import in.jlenterprises.ecommerce.dto.accounting.GstReturnDto;
import in.jlenterprises.ecommerce.dto.accounting.Gstr3bDto;

import java.time.LocalDate;
import java.util.UUID;

/** GST returns, e-way bill data, cash flow and outstanding/aging reports. */
public interface ComplianceService {

    GstReturnDto gstr1(LocalDate from, LocalDate to);   // outward supplies

    GstReturnDto gstr2(LocalDate from, LocalDate to);   // inward supplies

    Gstr3bDto gstr3b(LocalDate from, LocalDate to);

    CashFlowDto cashFlow(LocalDate from, LocalDate to);

    /** kind = RECEIVABLE (debtors) or PAYABLE (creditors). */
    AgingReportDto aging(String kind, LocalDate asOf);

    EwayBillDto ewayBill(UUID documentId);
}
