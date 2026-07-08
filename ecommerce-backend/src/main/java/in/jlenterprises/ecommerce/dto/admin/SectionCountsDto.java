package in.jlenterprises.ecommerce.dto.admin;

/**
 * "Needs attention" counts shown as badges next to the admin sidebar sections.
 * Each number reflects new/pending records; it drops as the admin actions them.
 */
public record SectionCountsDto(
        long ordersPending,
        long returnRequests,
        long exchangesPending,
        long serviceBookingsNew,
        long reviewsPending,
        long lowStock,
        long contactEnquiriesNew,
        long emiRequestsNew,
        long unreadNotifications
) {}
