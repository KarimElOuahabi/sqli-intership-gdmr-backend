package com.karim.gdmr_backend.notification.application;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.notification.domain.exception.NotificationNotFoundException;
import com.karim.gdmr_backend.notification.domain.exception.UnauthorizedNotificationAccessException;
import com.karim.gdmr_backend.notification.domain.model.Notification;
import com.karim.gdmr_backend.notification.domain.model.NotificationType;
import com.karim.gdmr_backend.notification.domain.port.in.CountUnreadNotificationsUseCase;
import com.karim.gdmr_backend.notification.domain.port.in.DeleteUserNotificationsUseCase;
import com.karim.gdmr_backend.notification.domain.port.in.ListNotificationsUseCase;
import com.karim.gdmr_backend.notification.domain.port.in.MarkNotificationReadUseCase;
import com.karim.gdmr_backend.notification.domain.port.in.SendNotificationUseCase;
import com.karim.gdmr_backend.notification.domain.port.out.NotificationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class NotificationService implements
        SendNotificationUseCase,
        ListNotificationsUseCase,
        CountUnreadNotificationsUseCase,
        MarkNotificationReadUseCase,
        DeleteUserNotificationsUseCase {

    private final NotificationRepositoryPort notificationRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    public NotificationService(NotificationRepositoryPort notificationRepository,
                                SseEmitterRegistry sseEmitterRegistry) {
        this.notificationRepository = notificationRepository;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @Override
    public Notification send(SendNotificationCommand command) {
        Notification notification = notificationRepository.save(Notification.createNew(
                command.recipientUserId(), command.type(), command.title(),
                command.message(), command.relatedVisitId(), command.relatedUserId()));

        sseEmitterRegistry.push(notification.getRecipientUserId(), SsePayload.from(notification));

        return notification;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean alreadySent(Long visitId, NotificationType type) {
        return notificationRepository.existsByRelatedVisitIdAndType(visitId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Notification> listForUser(ListNotificationsQuery query) {
        return notificationRepository.findAllForUser(query.userId(), query.unreadOnly(), query.types(), query.page(), query.size());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countUnread(userId);
    }

    @Override
    public Notification markRead(Long notificationId, Long requestingUserId) {
        Notification existing = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!existing.getRecipientUserId().equals(requestingUserId)) {
            throw new UnauthorizedNotificationAccessException(notificationId);
        }

        return notificationRepository.save(existing.withRead(true));
    }

    @Override
    public void markAllRead(Long userId) {
        PageResult<Notification> unread = notificationRepository.findAllForUser(userId, true, null, 0, 200);
        for (Notification n : unread.content()) {
            notificationRepository.save(n.withRead(true));
        }
    }

    @Override
    public void deleteForUser(Long userId) {
        notificationRepository.deleteAllForUser(userId);
    }

    /** What actually goes out over SSE — just enough for the client to update its badge/list. */
    private record SsePayload(
            Long id, NotificationType type, String title, String message,
            Long relatedVisitId, Long relatedUserId, boolean read, LocalDateTime createdAt) {
        static SsePayload from(Notification n) {
            return new SsePayload(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                    n.getRelatedVisitId(), n.getRelatedUserId(), n.isRead(), n.getCreatedAt());
        }
    }
}
