package in.jlenterprises.ecommerce.util;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient factory with REAL timeouts. {@code RestClient.create()} has none — a slow
 * external API (Razorpay, Supabase, WhatsApp) would pin the calling thread and, when
 * called inside a transaction, its DB connection too. With Hikari capped at 5 on the
 * Supabase free tier, a few hung calls used to be enough to exhaust the pool.
 */
public final class Http {

    private Http() {}

    /** 5s connect / 15s read — generous for payment/storage APIs, fatal for none. */
    public static RestClient client() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(5));
        f.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().requestFactory(f).build();
    }
}
