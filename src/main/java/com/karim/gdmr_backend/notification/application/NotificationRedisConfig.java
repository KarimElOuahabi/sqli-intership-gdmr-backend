package com.karim.gdmr_backend.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires the Redis pub/sub channel used to fan out notifications across backend instances.
 * Only matters once more than one instance is running — see {@code app.notifications.redis-enabled}.
 */
@Configuration
public class NotificationRedisConfig {

    private static final String NOTIFICATIONS_CHANNEL = "notifications";

    @Bean
    public ChannelTopic notificationsTopic() {
        return new ChannelTopic(NOTIFICATIONS_CHANNEL);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    // Spring Boot 4 only autoconfigures a bean of the newer tools.jackson.databind.ObjectMapper type;
    // this module needs the classic com.fasterxml one (already on the classpath via jjwt-jackson),
    // so it's provided explicitly here rather than relying on autoconfiguration.
    @Bean
    public ObjectMapper notificationObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // This is the piece that actually opens a live connection to Redis on startup — guarded so a
    // single-instance deployment (no Redis provisioned) doesn't fail to boot over a feature it isn't using.
    @Bean
    @ConditionalOnProperty(name = "app.notifications.redis-enabled", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            NotificationRedisSubscriber subscriber,
            ChannelTopic notificationsTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, notificationsTopic);
        return container;
    }
}
