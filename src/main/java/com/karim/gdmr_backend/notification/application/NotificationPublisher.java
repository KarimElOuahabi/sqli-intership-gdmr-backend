package com.karim.gdmr_backend.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

/**
 * Publishes a notification so it reaches the recipient's SSE connection — via Redis when this
 * deployment runs more than one instance (the recipient's connection could be held by any of
 * them), or directly to the local {@link SseEmitterRegistry} when it's just one instance, in
 * which case Redis is unnecessary infrastructure cost.
 *
 * Toggled with {@code app.notifications.redis-enabled} (defaults to on, matching local dev's
 * Docker Redis; set to false for a single-instance deployment with no Redis provisioned).
 */
@Component
public class NotificationPublisher {

    private final boolean redisEnabled;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic notificationsTopic;
    private final ObjectMapper objectMapper;

    public NotificationPublisher(
            @Value("${app.notifications.redis-enabled:true}") boolean redisEnabled,
            SseEmitterRegistry sseEmitterRegistry,
            StringRedisTemplate redisTemplate,
            ChannelTopic notificationsTopic,
            ObjectMapper objectMapper) {
        this.redisEnabled = redisEnabled;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplate = redisTemplate;
        this.notificationsTopic = notificationsTopic;
        this.objectMapper = objectMapper;
    }

    public void publish(NotificationPushMessage message) {
        if (!redisEnabled) {
            sseEmitterRegistry.push(message.recipientUserId(), message);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(notificationsTopic.getTopic(), json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification for publishing", e);
        }
    }
}
