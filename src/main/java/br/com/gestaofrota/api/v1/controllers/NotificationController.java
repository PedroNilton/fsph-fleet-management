package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.responses.UnreadNotificationCountResponse;
import br.com.gestaofrota.api.v1.dtos.responses.UserNotificationResponse;
import br.com.gestaofrota.application.services.NotificationService;
import br.com.gestaofrota.infra.security.models.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
    public List<UserNotificationResponse> listMine(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return notifications.listForRecipient(user.id(), unreadOnly).stream()
                .map(UserNotificationResponse::from)
                .toList();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
    public UnreadNotificationCountResponse countUnread(@AuthenticationPrincipal AuthenticatedUser user) {
        return new UnreadNotificationCountResponse(notifications.countUnread(user.id()));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
    public UserNotificationResponse markAsRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        return UserNotificationResponse.from(notifications.markAsRead(id, user.id()));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
    public Map<String, Integer> markAllAsRead(@AuthenticationPrincipal AuthenticatedUser user) {
        int marked = notifications.markAllAsRead(user.id());
        return Map.of("markedAsRead", marked);
    }
}
