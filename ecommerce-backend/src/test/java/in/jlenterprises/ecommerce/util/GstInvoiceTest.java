package in.jlenterprises.ecommerce.util;

import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.entity.AddressSnapshot;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.OrderItem;
import in.jlenterprises.ecommerce.mapper.OrderMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 4 books/GST correctness: per-line embedded GST at each item's snapshotted rate,
 * and IGST vs CGST/SGST decided by ship-to state. Electronics mix 18% and 28% slabs —
 * the old flat store-wide rate misstated the liability on every mixed order.
 */
class GstInvoiceTest {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("18");

    private static Order orderWith(String shipState, OrderItem... items) {
        Order o = new Order();
        o.setOrderNumber("ORD-TEST-1");
        o.setCurrency("INR");
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem i : items) {
            o.getItems().add(i);
            subtotal = subtotal.add(i.getLineTotal());
        }
        o.setSubtotal(subtotal);
        o.setDiscountTotal(BigDecimal.ZERO);
        o.setShippingTotal(BigDecimal.ZERO);
        o.setGrandTotal(subtotal);
        AddressSnapshot ship = new AddressSnapshot();
        ship.setState(shipState);
        o.setShippingAddress(ship);
        o.setBillingAddress(ship);
        return o;
    }

    private static OrderItem item(String lineTotal, String ratePercent) {
        OrderItem i = new OrderItem();
        i.setProductName("P");
        i.setQuantity(1);
        i.setUnitPrice(new BigDecimal(lineTotal));
        i.setLineTotal(new BigDecimal(lineTotal));
        if (ratePercent != null) i.setGstRate(new BigDecimal(ratePercent));
        return i;
    }

    // ── Per-line embedded GST ──

    @Test
    void mixedRatesComputePerLineNotFlat() {
        // ₹11,800 incl. 18% (tax 1,800) + ₹12,800 incl. 28% (tax 2,800) = tax 4,600.
        Order o = orderWith("Tamil Nadu", item("11800.00", "18"), item("12800.00", "28"));

        BigDecimal gst = GstUtil.embeddedGst(o, o.getGrandTotal(), DEFAULT_RATE);

        assertEquals(new BigDecimal("4600.00"), gst);
        // The old flat-18% computation would have said 3,752.54 — materially wrong.
    }

    @Test
    void legacyLinesWithoutSnapshotUseTheDefaultRate() {
        Order o = orderWith("Tamil Nadu", item("11800.00", null));
        assertEquals(new BigDecimal("1800.00"), GstUtil.embeddedGst(o, o.getGrandTotal(), DEFAULT_RATE));
    }

    @Test
    void discountShrinksEachLineShareProportionally() {
        // 10% off the whole order: each line's consideration is 90% of its total.
        Order o = orderWith("Tamil Nadu", item("11800.00", "18"), item("12800.00", "28"));
        o.setDiscountTotal(new BigDecimal("2460.00"));
        o.setGrandTotal(new BigDecimal("22140.00"));   // 24600 − 2460

        BigDecimal gst = GstUtil.embeddedGst(o, o.getGrandTotal(), DEFAULT_RATE);

        // 10620 incl. 18% → 1620; 11520 incl. 28% → 2520. Total 4140 (= 4600 × 0.9).
        assertEquals(new BigDecimal("4140.00"), gst);
    }

    @Test
    void shippingIsTaxedAtTheDefaultRate() {
        Order o = orderWith("Tamil Nadu", item("11800.00", "18"));
        o.setShippingTotal(new BigDecimal("99.00"));
        o.setGrandTotal(new BigDecimal("11899.00"));

        BigDecimal gst = GstUtil.embeddedGst(o, o.getGrandTotal(), DEFAULT_RATE);

        // 1800 (line) + 15.10 (embedded in ₹99 at 18%).
        assertEquals(new BigDecimal("1815.10"), gst);
    }

    // ── IGST vs CGST/SGST on the invoice ──

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void intraStateSplitsCgstSgstAndNeverIgst() {
        Order o = orderWith("Tamil Nadu", item("11800.00", "18"));
        InvoiceDto inv = mapper.toInvoice(o, DEFAULT_RATE, "GSTIN", "JL", "Addr", "Tamil Nadu");

        assertFalse(inv.interState());
        assertEquals(new BigDecimal("900.00"), inv.cgst());
        assertEquals(new BigDecimal("900.00"), inv.sgst());
        assertEquals(BigDecimal.ZERO, inv.igst());
        assertEquals(new BigDecimal("1800.00"), inv.taxTotal());
        assertEquals(new BigDecimal("10000.00"), inv.taxableValue());
    }

    @Test
    void interStateUsesIgstOnly() {
        Order o = orderWith("Kerala", item("11800.00", "18"));
        InvoiceDto inv = mapper.toInvoice(o, DEFAULT_RATE, "GSTIN", "JL", "Addr", "Tamil Nadu");

        assertTrue(inv.interState());
        assertEquals(new BigDecimal("1800.00"), inv.igst());
        assertEquals(BigDecimal.ZERO, inv.cgst());
        assertEquals(BigDecimal.ZERO, inv.sgst());
    }

    @Test
    void stateComparisonIsCaseAndSpaceInsensitive() {
        Order o = orderWith("  tamil nadu ", item("11800.00", "18"));
        InvoiceDto inv = mapper.toInvoice(o, DEFAULT_RATE, "GSTIN", "JL", "Addr", "Tamil Nadu");
        assertFalse(inv.interState(), "same state spelled differently must stay intra-state");
    }

    @Test
    void unknownShipStateDefaultsToIntraState() {
        Order o = orderWith(null, item("11800.00", "18"));
        InvoiceDto inv = mapper.toInvoice(o, DEFAULT_RATE, "GSTIN", "JL", "Addr", "Tamil Nadu");
        assertFalse(inv.interState(), "walk-in/unknown addresses are the store's home state");
    }
}
