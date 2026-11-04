package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.NotificationType;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_notifications")
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_request_id")
    private TripRequest tripRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserNotification() {
    }

    public UserNotification(
            User recipient,
            TripRequest tripRequest,
            NotificationType type,
            String title,
            String message
    ) {
        if (recipient == null) {
            throw new InvalidRequestException("O destinatário da notificação é obrigatório");
        }
        if (type == null) {
            throw new InvalidRequestException("O tipo da notificação é obrigatório");
        }
        if (title == null || title.isBlank()) {
            throw new InvalidRequestException("O título da notificação é obrigatório");
        }
        if (message == null || message.isBlank()) {
            throw new InvalidRequestException("A mensagem da notificação é obrigatória");
        }
        this.recipient = recipient;
        this.tripRequest = tripRequest;
        this.type = type;
        this.title = title.trim();
        this.message = message.trim();
    }

    public void markAsRead(Instant readAt) {
        if (!this.read) {
            this.read = true;
            this.readAt = Objects.requireNonNull(readAt, "Instante de leitura é obrigatório");
        }
    }

    public void markEmailSent() {
        this.emailSent = true;
    }

    public UUID getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public TripRequest getTripRequest() {
        return tripRequest;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
