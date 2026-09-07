package com.karim.gdmr_backend.notification.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, single-use tickets that let an EventSource (which can't send an
 * Authorization header) authenticate the SSE stream without ever putting the real
 * JWT in a URL/log. Issued via a normal Bearer-authenticated call, consumed once.
 *
 * Backed by Redis when this deployment runs more than one instance — the "get a
 * ticket" call and the "open the stream" call can land on different instances, so
 * the ticket has to be visible to whichever one ends up consuming it. With a single
 * instance (see {@code app.notifications.redis-enabled}), both calls always land on
 * the same process, so a plain local map works and Redis isn't needed for this either.
 */
@Component
public class SseTicketService {

    private static final Duration TICKET_TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "sse-ticket:";

    private final boolean redisEnabled;
    private final StringRedisTemplate redisTemplate;

    private record TicketEntry(Long userId, Instant expiresAt) {}
    private final Map<String, TicketEntry> localTickets = new ConcurrentHashMap<>();

    public SseTicketService(@Value("${app.notifications.redis-enabled:true}") boolean redisEnabled,
                             StringRedisTemplate redisTemplate) {
        this.redisEnabled = redisEnabled;
        this.redisTemplate = redisTemplate;
    }

    public String issueTicket(Long userId) {
        String ticket = UUID.randomUUID().toString();
        if (redisEnabled) {
            redisTemplate.opsForValue().set(KEY_PREFIX + ticket, userId.toString(), TICKET_TTL);
        } else {
            purgeExpired();
            localTickets.put(ticket, new TicketEntry(userId, Instant.now().plus(TICKET_TTL)));
        }
        return ticket;
    }

    public Optional<Long> consumeTicket(String ticket) {
        if (redisEnabled) {
            String userId = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + ticket);
            return Optional.ofNullable(userId).map(Long::valueOf);
        }
        TicketEntry entry = localTickets.remove(ticket);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        localTickets.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }
}
