package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** A "Contact Us" enquiry submitted from the storefront. */
@Entity
@Table(name = "contact_enquiries")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class ContactEnquiry extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "subject", length = 160)
    private String subject;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    /** Workflow status (own column — BaseEntity owns a generic `status`). NEW → READ → CLOSED. */
    @Column(name = "enquiry_status", nullable = false, length = 20)
    private String enquiryStatus = "NEW";
}
