package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** A repair / installation / AMC service request submitted from the storefront. */
@Entity
@Table(name = "service_bookings")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class ServiceBooking extends BaseEntity {

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /** e.g. AC service, TV repair, installation, AMC. */
    @Column(name = "service_type", length = 80)
    private String serviceType;

    @Column(name = "message", length = 1000)
    private String message;

    /** Free-text preferred date/time from the customer (kept as-is). */
    @Column(name = "preferred_date", length = 60)
    private String preferredDate;

    /** Workflow status (own column — BaseEntity already owns a generic `status`).
        NEW → CONTACTED → SCHEDULED → DONE / CANCELLED. */
    @Column(name = "booking_status", nullable = false, length = 20)
    private String bookingStatus = "NEW";
}
