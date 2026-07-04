package in.jlenterprises.ecommerce.storage;

import in.jlenterprises.ecommerce.config.AppProperties;
import in.jlenterprises.ecommerce.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;

/**
 * Uploads product images to a Supabase Storage bucket via its REST API and
 * returns the public URL. The backend stores only the URL (on {@code ProductImage});
 * the bytes live in Supabase Storage (Render's disk is ephemeral).
 *
 * Config: {@code app.supabase.url}, {@code app.supabase.service-key} (service_role
 * key — server-side only), {@code app.supabase.bucket} (a PUBLIC bucket). If url/key
 * are blank, uploads are rejected with a clear message rather than failing obscurely.
 */
@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final AppProperties.Supabase cfg;
    private final RestClient http = RestClient.create();

    public SupabaseStorageService(AppProperties props) {
        this.cfg = props.supabase();
    }

    public boolean isConfigured() {
        return cfg != null && cfg.configured();
    }

    /**
     * Upload bytes to {bucket}/{objectPath} (upsert) and return the public URL.
     * objectPath must be a bucket-relative key, e.g. {@code products/<id>/<uuid>.jpg}.
     */
    public String upload(String objectPath, byte[] bytes, String contentType) {
        if (!isConfigured()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Image storage is not configured. Set SUPABASE_URL and SUPABASE_SERVICE_KEY on the server.");
        }
        String base = apiBase(cfg.url());
        String full = base + "/storage/v1/object/" + cfg.bucket() + "/" + objectPath;
        try {
            // Supabase Storage uses POST to CREATE an object (PUT only replaces an
            // existing one). Both the service-role Bearer AND the apikey header are
            // required by the Supabase gateway (Kong).
            http.post()
                    .uri(URI.create(full))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.serviceKey())
                    .header("apikey", cfg.serviceKey())
                    .header("x-upsert", "true")
                    .contentType(contentType == null || contentType.isBlank()
                            ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // Log the exact status + Supabase response so failures are diagnosable in Render logs.
            log.warn("Supabase upload failed for {}: HTTP {} {}", objectPath,
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Image upload failed. Please try again.");
        } catch (Exception e) {
            log.warn("Supabase upload failed for {}: {}", objectPath, e.toString());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Image upload failed. Please try again.");
        }
        return base + "/storage/v1/object/public/" + cfg.bucket() + "/" + objectPath;
    }

    /** Best-effort delete of an object given its public URL. Never throws. */
    public void deleteByPublicUrl(String publicUrl) {
        if (!isConfigured() || publicUrl == null) return;
        String marker = "/storage/v1/object/public/" + cfg.bucket() + "/";
        int i = publicUrl.indexOf(marker);
        if (i < 0) return;   // not one of our objects
        String objectPath = publicUrl.substring(i + marker.length());
        try {
            http.delete()
                    .uri(URI.create(apiBase(cfg.url()) + "/storage/v1/object/" + cfg.bucket() + "/" + objectPath))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.serviceKey())
                    .header("apikey", cfg.serviceKey())
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Supabase delete failed for {} (ignored): {}", objectPath, e.getMessage());
        }
    }

    private static String trimSlash(String s) {
        return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }

    /**
     * Normalise the configured Supabase URL to just {@code scheme://host[:port]},
     * dropping any path someone may have pasted (e.g. a trailing {@code /rest/v1}),
     * so the storage path is always built correctly and reaches the Storage API
     * rather than PostgREST.
     */
    private static String apiBase(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        try {
            URI u = URI.create(s);
            if (u.getHost() != null) {
                String b = (u.getScheme() == null ? "https" : u.getScheme()) + "://" + u.getHost();
                if (u.getPort() > 0) b += ":" + u.getPort();
                return b;
            }
        } catch (Exception ignored) {
            // fall through to a simple trim
        }
        return trimSlash(s);
    }
}
