package com.university.assets.notification;

import com.university.assets.common.exception.ApiException;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
import com.university.assets.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    public record NotificationResponse(UUID id, String type, String title, String message,
                                       String entityType, UUID entityId, Instant readAt, Instant createdAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                    n.getEntityType(), n.getEntityId(), n.getReadAt(), n.getCreatedAt());
        }
    }

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(
                repository.findByUserIdOrderByCreatedAtDesc(CurrentUser.id(), pageable),
                NotificationResponse::from));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count", repository.countByUserIdAndReadAtIsNull(CurrentUser.id())));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ApiResponse<Void> markRead(@PathVariable UUID id) {
        Notification notification = owned(id);
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            repository.save(notification);
        }
        return ApiResponse.message("Notification marked as read");
    }

    @PostMapping("/read-all")
    @Transactional
    public ApiResponse<Void> markAllRead() {
        repository.markAllRead(CurrentUser.id(), Instant.now());
        return ApiResponse.message("All notifications marked as read");
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        repository.delete(owned(id));
        return ApiResponse.message("Notification deleted");
    }

    private Notification owned(UUID id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Notification"));
        if (!notification.getUserId().equals(CurrentUser.id())) {
            throw ApiException.forbidden("You can only manage your own notifications");
        }
        return notification;
    }
}
