package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.dto.customer.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    Page<NotificationDto> list(UUID userId, Pageable pageable);

    long unreadCount(UUID userId);

    void markRead(UUID userId, UUID notificationId);

    void markAllRead(UUID userId);

    /** Server-side helper used by other modules (e.g. order placed) to notify a user. */
    void notifyUser(UUID userId, NotificationType type, String title, String message, String link);

    /** Notify every admin / super-admin (e.g. new order, low stock). Best-effort; never throws. */
    void notifyAdmins(NotificationType type, String title, String message, String link);

    /**
     * Notify every admin / super-admin, with extra context so the admin UI can show the
     * section and deep-link to the related record. Best-effort; never throws.
     */
    void notifyAdmins(NotificationType type, String title, String message, String link,
                      String section, UUID relatedId, String relatedType);
}
