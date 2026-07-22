package in.jlenterprises.ecommerce.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Which requests the IP throttle covers — including the GET tracking endpoint added in
    audit #2 (order tracking validates order number + phone last-10; unthrottled, the
    phone could be brute-forced for any known order number). */
class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(15, 60);

    private static HttpServletRequest req(String method, String uri) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getMethod()).thenReturn(method);
        when(r.getRequestURI()).thenReturn(uri);
        return r;
    }

    @Test
    void throttlesLoginPostAndTrackingGet() {
        assertFalse(filter.shouldNotFilter(req("POST", "/api/v1/auth/login")));
        assertFalse(filter.shouldNotFilter(req("GET", "/api/v1/orders/track")));
    }

    @Test
    void leavesNormalTrafficAlone() {
        assertTrue(filter.shouldNotFilter(req("GET", "/api/v1/products")));
        assertTrue(filter.shouldNotFilter(req("GET", "/api/v1/auth/login")));      // GET login isn't a thing
        assertTrue(filter.shouldNotFilter(req("POST", "/api/v1/orders/track")));   // only the GET is public
        assertTrue(filter.shouldNotFilter(req("PUT", "/api/v1/cart/items/x")));
    }
}
