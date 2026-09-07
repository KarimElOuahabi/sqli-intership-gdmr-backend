package com.karim.gdmr_backend.notification.adapter.in.web;

import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.notification.application.SseEmitterRegistry;
import com.karim.gdmr_backend.notification.application.SseTicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Live and REST notification delivery: list, unread count, mark read, SSE stream")
public class NotificationStreamController {

    private final SseTicketService sseTicketService;
    private final SseEmitterRegistry sseEmitterRegistry;

    public NotificationStreamController(SseTicketService sseTicketService, SseEmitterRegistry sseEmitterRegistry) {
        this.sseTicketService = sseTicketService;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    // Authenticated normally (Bearer header) — hands back a throwaway ticket the
    // EventSource can use, since EventSource can't send an Authorization header.
    @PostMapping("/stream-ticket")
    public ResponseEntity<Map<String, String>> issueTicket(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).userId();
        String ticket = sseTicketService.issueTicket(userId);
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }

    // Not behind the normal Bearer filter (see SecurityConfig) — the ticket itself
    // is the credential here, single-use and expires in 30s.
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String ticket) {
        Long userId = sseTicketService.consumeTicket(ticket)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired ticket"));

        SseEmitter emitter = new SseEmitter(0L);
        sseEmitterRegistry.register(userId, emitter);
        return emitter;
    }
}
