package com.karim.gdmr_backend.notification.domain.port.out;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.notification.domain.model.Notification;
import com.karim.gdmr_backend.notification.domain.model.NotificationType;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    PageResult<Notification> findAllForUser(Long userId, Boolean unreadOnly, List<NotificationType> types, int page, int size);
    long countUnread(Long userId);
    boolean existsByRelatedVisitIdAndType(Long visitId, NotificationType type);

    void deleteAllForUser(Long userId);
}
