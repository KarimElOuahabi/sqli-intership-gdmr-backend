package com.karim.gdmr_backend.notification.application;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived, single-use tickets that let an EventSource (which can't send an
 * Authorization header) authenticate the SSE stream without ever putting the real
 * JWT in a URL/log. Issued via a normal Bearer-authenticated call, consumed once.
 *
 * Backed by Redis rather than a local map: behind a load balancer, the "get a ticket"
 * call and the "open the stream" call can land on different instances, so the ticket
 * has to be visible to whichever instance ends up consuming it.
 */
@Component
public class SseTicketService {

    private static final Duration TICKET_TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "sse-ticket:";

    private final StringRedisTemplate redisTemplate;

    public SseTicketService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String issueTicket(Long userId) {
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + ticket, userId.toString(), TICKET_TTL);
        return ticket;
    }

    public Optional<Long> consumeTicket(String ticket) {
        String userId = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + ticket);
        return Optional.ofNullable(userId).map(Long::valueOf);
    }
}
