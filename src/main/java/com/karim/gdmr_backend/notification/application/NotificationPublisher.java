package com.karim.gdmr_backend.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

/**
 * Publishes a notification to Redis instead of pushing it to {@link SseEmitterRegistry} directly —
 * behind a load balancer, the recipient's open SSE connection can be held by any backend instance,
 * not necessarily the one that created the notification. Every instance subscribes to the same
 * channel via {@link NotificationRedisSubscriber}, so whichever one actually holds that user's
 * connection is the one that delivers it.
 */
@Component
public class NotificationPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic notificationsTopic;
    private final ObjectMapper objectMapper;

    public NotificationPublisher(StringRedisTemplate redisTemplate, ChannelTopic notificationsTopic, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.notificationsTopic = notificationsTopic;
        this.objectMapper = objectMapper;
    }

    public void publish(NotificationPushMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(notificationsTopic.getTopic(), json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification for publishing", e);
        }
    }
}
