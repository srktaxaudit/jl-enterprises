package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.dto.customer.NotificationDto;
import in.jlenterprises.ecommerce.entity.Notification;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.NotificationMapper;
import in.jlenterprises.ecommerce.repository.NotificationRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

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
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        notificationRepository.save(n);
    }
}
