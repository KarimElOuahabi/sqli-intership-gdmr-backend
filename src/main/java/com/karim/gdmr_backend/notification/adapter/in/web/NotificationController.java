package com.karim.gdmr_backend.notification.adapter.in.web;

import com.karim.gdmr_backend.auth.adapter.in.web.dto.PagedResponse;
import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.notification.adapter.in.web.dto.NotificationResponse;
import com.karim.gdmr_backend.notification.domain.model.Notification;
import com.karim.gdmr_backend.notification.domain.model.NotificationType;
import com.karim.gdmr_backend.notification.domain.port.in.CountUnreadNotificationsUseCase;
import com.karim.gdmr_backend.notification.domain.port.in.ListNotificationsUseCase;
import com.karim.gdmr_backend.notification.domain.port.in.MarkNotificationReadUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Live and REST notification delivery: list, unread count, mark read, SSE stream")
public class NotificationController {

    private final ListNotificationsUseCase listNotificationsUseCase;
    private final CountUnreadNotificationsUseCase countUnreadNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;

    public NotificationController(ListNotificationsUseCase listNotificationsUseCase,
                                   CountUnreadNotificationsUseCase countUnreadNotificationsUseCase,
                                   MarkNotificationReadUseCase markNotificationReadUseCase) {
        this.listNotificationsUseCase = listNotificationsUseCase;
        this.countUnreadNotificationsUseCase = countUnreadNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) List<NotificationType> types,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = userId(authentication);
        PageResult<Notification> result = listNotificationsUseCase.listForUser(
                new ListNotificationsUseCase.ListNotificationsQuery(userId, unreadOnly, types, page, size));

        PageResult<NotificationResponse> mapped = new PageResult<>(
                result.content().stream().map(NotificationResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());

        return ResponseEntity.ok(PagedResponse.from(mapped));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication authentication) {
        long count = countUnreadNotificationsUseCase.countUnread(userId(authentication));
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(Authentication authentication, @PathVariable Long id) {
        Notification n = markNotificationReadUseCase.markRead(id, userId(authentication));
        return ResponseEntity.ok(NotificationResponse.from(n));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        markNotificationReadUseCase.markAllRead(userId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long userId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).userId();
    }
}
