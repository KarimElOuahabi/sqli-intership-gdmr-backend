package com.karim.gdmr_backend.notification.adapter.out.persistence;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.notification.domain.model.Notification;
import com.karim.gdmr_backend.notification.domain.model.NotificationType;
import com.karim.gdmr_backend.notification.domain.port.out.NotificationRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpaRepository;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = new NotificationEntity(
                notification.getId(),
                notification.getRecipientUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRelatedVisitId(),
                notification.getRelatedUserId(),
                notification.isRead(),
                LocalDateTime.now()
        );
        NotificationEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public PageResult<Notification> findAllForUser(Long userId, Boolean unreadOnly, List<NotificationType> types, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = jpaRepository.search(userId, unreadOnly != null && unreadOnly,
                (types == null || types.isEmpty()) ? null : types, pageable);

        List<Notification> content = result.getContent().stream().map(this::toDomain).toList();
        return new PageResult<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public long countUnread(Long userId) {
        return jpaRepository.countByRecipientUserIdAndReadFalse(userId);
    }

    @Override
    public boolean existsByRelatedVisitIdAndType(Long visitId, NotificationType type) {
        return jpaRepository.existsByRelatedVisitIdAndType(visitId, type);
    }

    @Override
    public void deleteAllForUser(Long userId) {
        jpaRepository.clearRelatedUserId(userId);
        jpaRepository.deleteByRecipientUserId(userId);
    }

    private Notification toDomain(NotificationEntity entity) {
        return new Notification(entity.getId(), entity.getRecipientUserId(), entity.getType(),
                entity.getTitle(), entity.getMessage(), entity.getRelatedVisitId(),
                entity.getRelatedUserId(), entity.isRead(), entity.getCreatedAt());
    }
}
