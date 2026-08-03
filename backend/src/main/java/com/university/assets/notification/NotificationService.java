package com.university.assets.notification;

import com.university.assets.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final MailService mailService;

    public NotificationService(NotificationRepository repository, UserRepository userRepository,
                               MailService mailService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void notifyUser(UUID userId, String type, String title, String message,
                           String entityType, UUID entityId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        repository.save(notification);
        userRepository.findById(userId).ifPresent(user ->
                mailService.send(user.getEmail(), title, message));
    }

    /** Scheduled-job variant that avoids re-sending the same alert within a day. */
    @Transactional
    public void notifyUserOncePerDay(UUID userId, String type, String title, String message,
                                     String entityType, UUID entityId) {
        Instant since = Instant.now().minus(1, ChronoUnit.DAYS);
        if (repository.existsByUserIdAndTypeAndEntityIdAndCreatedAtAfter(userId, type, entityId, since)) {
            return;
        }
        notifyUser(userId, type, title, message, entityType, entityId);
    }
}
