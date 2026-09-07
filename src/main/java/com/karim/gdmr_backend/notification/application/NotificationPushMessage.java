package com.karim.gdmr_backend.notification.application;

import com.karim.gdmr_backend.notification.domain.model.Notification;
import com.karim.gdmr_backend.notification.domain.model.NotificationType;

import java.time.LocalDateTime;

/** What gets published to Redis and, unchanged, forwarded to the browser over SSE —
 * just enough for the client to update its badge/list. */
public record NotificationPushMessage(
        Long recipientUserId, Long id, NotificationType type, String title, String message,
        Long relatedVisitId, Long relatedUserId, boolean read, LocalDateTime createdAt) {

    static NotificationPushMessage from(Notification n) {
        return new NotificationPushMessage(n.getRecipientUserId(), n.getId(), n.getType(), n.getTitle(),
                n.getMessage(), n.getRelatedVisitId(), n.getRelatedUserId(), n.isRead(), n.getCreatedAt());
    }
}
