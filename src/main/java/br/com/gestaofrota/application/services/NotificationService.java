package br.com.gestaofrota.application.services;

import br.com.gestaofrota.domain.enums.NotificationType;
import br.com.gestaofrota.domain.exceptions.ResourceNotFoundException;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.TripRequest;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.UserNotification;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.UserNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final DateTimeFormatter PERIOD_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final UserNotificationRepository notifications;
    private final NotificationEmailSender emailSender;

    public NotificationService(
            UserNotificationRepository notifications,
            NotificationEmailSender emailSender
    ) {
        this.notifications = notifications;
        this.emailSender = emailSender;
    }

    @Transactional
    public UserNotification notifyTripRequestApproved(TripRequest request) {
        String title = "Solicitação de viagem aprovada";
        StringBuilder body = new StringBuilder()
                .append("Sua solicitação de viagem de ")
                .append(request.getOrigin())
                .append(" para ")
                .append(request.getDestination())
                .append(" (saída em ")
                .append(PERIOD_FORMATTER.format(request.getDepartureAt()))
                .append(") foi aprovada.");

        Vehicle vehicle = request.getAssignedVehicle();
        if (vehicle != null) {
            body.append(" Veículo pré-reservado: ")
                    .append(vehicle.getModel())
                    .append(" (placa ")
                    .append(vehicle.getPlate())
                    .append(").");
        }
        if (request.getDecisionNotes() != null && !request.getDecisionNotes().isBlank()) {
            body.append(" Observação: ").append(request.getDecisionNotes());
        }

        return saveAndDispatch(
                request.getRequester(),
                request,
                NotificationType.TRIP_REQUEST_APPROVED,
                title,
                truncate(body.toString(), 1000)
        );
    }

    @Transactional
    public UserNotification notifyTripRequestRejected(TripRequest request) {
        String title = "Solicitação de viagem recusada";
        String body = "Sua solicitação de viagem de "
                + request.getOrigin()
                + " para "
                + request.getDestination()
                + " (saída em "
                + PERIOD_FORMATTER.format(request.getDepartureAt())
                + ") foi recusada. Motivo: "
                + request.getRejectionReason();

        return saveAndDispatch(
                request.getRequester(),
                request,
                NotificationType.TRIP_REQUEST_REJECTED,
                title,
                truncate(body, 1000)
        );
    }

    @Transactional
    public UserNotification notifyTripRequestScheduled(TripRequest request) {
        Vehicle vehicle = request.getAssignedVehicle();
        Driver driver = request.getAssignedDriver();
        String vehicleDescription = vehicle != null
                ? vehicle.getModel() + " (placa " + vehicle.getPlate() + ")"
                : "definido";
        String driverName = driver != null && driver.getUser() != null
                ? driver.getUser().getName()
                : "definido";

        String title = "Viagem agendada com veículo e motorista";
        String body = "Sua viagem de "
                + request.getOrigin()
                + " para "
                + request.getDestination()
                + " foi agendada para o período de "
                + PERIOD_FORMATTER.format(request.getDepartureAt())
                + " até "
                + PERIOD_FORMATTER.format(request.getExpectedReturnAt())
                + ". Veículo: "
                + vehicleDescription
                + ". Motorista: "
                + driverName
                + ".";

        return saveAndDispatch(
                request.getRequester(),
                request,
                NotificationType.TRIP_REQUEST_SCHEDULED,
                title,
                truncate(body, 1000)
        );
    }

    @Transactional(readOnly = true)
    public List<UserNotification> listForRecipient(UUID recipientUserId, boolean unreadOnly) {
        if (unreadOnly) {
            return notifications.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientUserId);
        }
        return notifications.findByRecipientIdOrderByCreatedAtDesc(recipientUserId);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID recipientUserId) {
        return notifications.countByRecipientIdAndReadFalse(recipientUserId);
    }

    @Transactional
    public UserNotification markAsRead(UUID notificationId, UUID recipientUserId) {
        UserNotification notification = notifications.findByIdAndRecipientId(notificationId, recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação", notificationId));
        notification.markAsRead(Instant.now());
        return notification;
    }

    @Transactional
    public int markAllAsRead(UUID recipientUserId) {
        List<UserNotification> unread = notifications.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientUserId);
        Instant now = Instant.now();
        unread.forEach(item -> item.markAsRead(now));
        return unread.size();
    }

    private UserNotification saveAndDispatch(
            User recipient,
            TripRequest tripRequest,
            NotificationType type,
            String title,
            String message
    ) {
        UserNotification notification = notifications.save(new UserNotification(
                recipient,
                tripRequest,
                type,
                truncate(title, 160),
                message
        ));

        if (emailSender.sendEmail(recipient.getEmail(), notification.getTitle(), notification.getMessage())) {
            notification.markEmailSent();
        }
        return notification;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
