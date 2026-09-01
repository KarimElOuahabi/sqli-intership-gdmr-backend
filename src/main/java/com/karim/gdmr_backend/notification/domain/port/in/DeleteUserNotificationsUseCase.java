package com.karim.gdmr_backend.notification.domain.port.in;

/** Purges a user's own notifications and detaches them as a "related user" from anyone else's. */
public interface DeleteUserNotificationsUseCase {
    void deleteForUser(Long userId);
}
