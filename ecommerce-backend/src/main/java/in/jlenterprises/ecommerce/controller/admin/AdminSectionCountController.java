package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.ExchangeStatus;
import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.ReviewStatus;
import in.jlenterprises.ecommerce.dto.admin.SectionCountsDto;
import in.jlenterprises.ecommerce.repository.ContactEnquiryRepository;
import in.jlenterprises.ecommerce.repository.EmiRequestRepository;
import in.jlenterprises.ecommerce.repository.ExchangeRequestRepository;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.repository.NotificationRepository;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.ReviewRepository;
import in.jlenterprises.ecommerce.repository.ServiceBookingRepository;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * "Needs attention" counts for the sidebar badges. Read-only aggregate over the
 * existing repositories — no new tables. Any signed-in staff member may read it;
 * the badges simply mirror what the sidebar already shows.
 */
@RestController
@RequestMapping("/api/v1/admin/section-counts")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','ORDER_MANAGER','PRODUCT_MANAGER'," +
        "'INVENTORY_MANAGER','MARKETING_MANAGER','CUSTOMER_SUPPORT','ACCOUNTANT','HR')")
@Tag(name = "Admin — Section counts", description = "Pending/new counts for sidebar badges")
public class AdminSectionCountController {

    private final OrderRepository orderRepository;
    private final ExchangeRequestRepository exchangeRepository;
    private final ServiceBookingRepository serviceBookingRepository;
    private final ReviewRepository reviewRepository;
    private final InventoryRepository inventoryRepository;
    private final NotificationRepository notificationRepository;
    private final ContactEnquiryRepository contactEnquiryRepository;
    private final EmiRequestRepository emiRequestRepository;

    public AdminSectionCountController(OrderRepository orderRepository,
                                       ExchangeRequestRepository exchangeRepository,
                                       ServiceBookingRepository serviceBookingRepository,
                                       ReviewRepository reviewRepository,
                                       InventoryRepository inventoryRepository,
                                       NotificationRepository notificationRepository,
                                       ContactEnquiryRepository contactEnquiryRepository,
                                       EmiRequestRepository emiRequestRepository) {
        this.orderRepository = orderRepository;
        this.exchangeRepository = exchangeRepository;
        this.serviceBookingRepository = serviceBookingRepository;
        this.reviewRepository = reviewRepository;
        this.inventoryRepository = inventoryRepository;
        this.notificationRepository = notificationRepository;
        this.contactEnquiryRepository = contactEnquiryRepository;
        this.emiRequestRepository = emiRequestRepository;
    }

    @GetMapping
    @Operation(summary = "New/pending counts for the admin sidebar badges")
    public ApiResponse<SectionCountsDto> counts() {
        long unread = notificationRepository.countByUserIdAndReadFalse(SecurityUtils.currentUserId());
        SectionCountsDto dto = new SectionCountsDto(
                orderRepository.countByOrderStatus(OrderStatus.PENDING),
                orderRepository.countByOrderStatus(OrderStatus.RETURN_REQUESTED),
                exchangeRepository.countByExchangeStatusIn(List.of(ExchangeStatus.PENDING, ExchangeStatus.UNDER_REVIEW)),
                serviceBookingRepository.countByBookingStatus("NEW"),
                reviewRepository.countByReviewStatus(ReviewStatus.PENDING),
                inventoryRepository.countLowStock(),
                contactEnquiryRepository.countByEnquiryStatus("NEW"),
                emiRequestRepository.countByEmiStatus("NEW"),
                unread
        );
        return ApiResponse.success(dto);
    }
}
