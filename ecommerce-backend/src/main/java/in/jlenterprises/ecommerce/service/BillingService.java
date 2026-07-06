package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.dto.admin.BillingRowDto;
import in.jlenterprises.ecommerce.dto.admin.BillingSummaryDto;
import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.dto.order.OrderPaymentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

/** Admin billing: invoices/payments ledger, revenue &amp; GST summary, and payment actions. */
public interface BillingService {

    Page<BillingRowDto> listInvoices(PaymentStatus status, PaymentMethod method, String q,
                                     Instant from, Instant to, Pageable pageable);

    BillingSummaryDto summary(Instant from, Instant to);

    /** Full GST invoice for any order (admin — not user-scoped). */
    InvoiceDto adminInvoice(UUID orderId);

    /** Mark an order's payment as paid (e.g. a COD collected / manual/offline payment). */
    OrderPaymentDto markPaid(UUID orderId);
}
