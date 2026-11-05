package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.NotificationType;
import br.com.gestaofrota.domain.models.TripRequest;
import br.com.gestaofrota.domain.models.UserNotification;

import java.time.Instant;
import java.util.UUID;

public record UserNotificationResponse(
        UUID id,
        UUID tripRequestId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant readAt,
        boolean emailSent,
        Instant createdAt
) {
    public static UserNotificationResponse from(UserNotification notification) {
        TripRequest tripRequest = notification.getTripRequest();
        return new UserNotificationResponse(
                notification.getId(),
                tripRequest != null ? tripRequest.getId() : null,
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getReadAt(),
                notification.isEmailSent(),
                notification.getCreatedAt()
        );
    }
}
