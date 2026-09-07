package com.karim.gdmr_backend.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Runs on every backend instance. Whichever instance actually holds the recipient's SSE
 * connection is the one where {@link SseEmitterRegistry#push} has any effect — the others
 * receive the same message, find no matching emitter, and it's a no-op.
 */
@Component
public class NotificationRedisSubscriber implements MessageListener {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;

    public NotificationRedisSubscriber(SseEmitterRegistry sseEmitterRegistry, ObjectMapper objectMapper) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationPushMessage payload = objectMapper.readValue(message.getBody(), NotificationPushMessage.class);
            sseEmitterRegistry.push(payload.recipientUserId(), payload);
        } catch (Exception e) {
            System.err.println("Failed to process notification pub/sub message: " + e.getMessage());
        }
    }
}
