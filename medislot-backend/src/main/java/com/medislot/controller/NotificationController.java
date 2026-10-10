package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.NotificationDtos.NotificationView;
import com.medislot.dto.NotificationDtos.UnreadCount;
import com.medislot.security.AuthUser;
import com.medislot.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The caller's own inbox. Any logged-in role may use it; every query is limited to the caller's user id. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<NotificationView>> list(@AuthenticationPrincipal AuthUser actor,
                                                    @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ApiResponse.ok("OK", service.list(actor.id(), unreadOnly));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCount> unread(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", new UnreadCount(service.unreadCount(actor.id())));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> read(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        service.markRead(actor.id(), id);
        return ApiResponse.ok("OK", null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> readAll(@AuthenticationPrincipal AuthUser actor) {
        service.markAllRead(actor.id());
        return ApiResponse.ok("OK", null);
    }
}