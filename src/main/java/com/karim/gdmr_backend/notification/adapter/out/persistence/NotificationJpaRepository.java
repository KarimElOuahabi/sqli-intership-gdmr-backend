package com.karim.gdmr_backend.notification.adapter.out.persistence;

import com.karim.gdmr_backend.notification.domain.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    @Query("""
        SELECT n FROM NotificationEntity n
        WHERE n.recipientUserId = :userId
        AND (:unreadOnly = FALSE OR n.read = FALSE)
        AND (:types IS NULL OR n.type IN :types)
        ORDER BY n.createdAt DESC
        """)
    Page<NotificationEntity> search(@Param("userId") Long userId,
                                     @Param("unreadOnly") boolean unreadOnly,
                                     @Param("types") List<NotificationType> types,
                                     Pageable pageable);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);

    boolean existsByRelatedVisitIdAndType(Long relatedVisitId, NotificationType type);

    void deleteByRecipientUserId(Long recipientUserId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.relatedUserId = NULL WHERE n.relatedUserId = :userId")
    void clearRelatedUserId(@Param("userId") Long userId);
}
