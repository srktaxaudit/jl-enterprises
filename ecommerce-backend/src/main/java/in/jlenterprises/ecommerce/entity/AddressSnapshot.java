package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An immutable copy of an address embedded on an order. Snapshotting keeps the
 * order's shipping/billing details intact even if the user later edits or
 * deletes the source {@link Address}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class AddressSnapshot {

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "line1", length = 200)
    private String line1;

    @Column(name = "line2", length = 200)
    private String line2;

    @Column(name = "city", length = 80)
    private String city;

    @Column(name = "state", length = 80)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 80)
    private String country;
}
