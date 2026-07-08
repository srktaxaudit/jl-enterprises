package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.dto.customer.NotificationDto;
import in.jlenterprises.ecommerce.entity.Notification;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.NotificationMapper;
import in.jlenterprises.ecommerce.repository.NotificationRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final List<RoleName> ADMIN_ROLES = List.of(RoleName.ROLE_SUPER_ADMIN, RoleName.ROLE_ADMIN);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository, NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> list(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, Instant.now());
    }

    @Override
    @Transactional
    public void notifyUser(UUID userId, NotificationType type, String title, String message, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        save(user, type, title, message, link, null, null, null);
    }

    @Override
    @Transactional
    public void notifyAdmins(NotificationType type, String title, String message, String link) {
        notifyAdmins(type, title, message, link, null, null, null);
    }

    @Override
    @Transactional
    public void notifyAdmins(NotificationType type, String title, String message, String link,
                             String section, UUID relatedId, String relatedType) {
        try {
            for (User admin : userRepository.findByRoleNames(ADMIN_ROLES)) {
                save(admin, type, title, message, link, section, relatedId, relatedType);
            }
        } catch (Exception e) {
            // Admin alerts are best-effort — never break the triggering business action.
            log.warn("Failed to notify admins ({}): {}", title, e.getMessage());
        }
    }

    private void save(User user, NotificationType type, String title, String message, String link,
                      String section, UUID relatedId, String relatedType) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        n.setSection(section);
        n.setRelatedId(relatedId);
        n.setRelatedType(relatedType);
        notificationRepository.save(n);
    }
}
