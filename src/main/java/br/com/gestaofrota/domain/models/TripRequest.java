package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.TripRequestStatus;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "trip_requests")
public class TripRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @Column(name = "departure_at", nullable = false)
    private Instant departureAt;

    @Column(name = "expected_return_at", nullable = false)
    private Instant expectedReturnAt;

    @Column(nullable = false, length = 200)
    private String origin;

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_vehicle_id")
    private Vehicle requestedVehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripRequestStatus status = TripRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_vehicle_id")
    private Vehicle assignedVehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
    private Driver assignedDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "decision_notes", length = 500)
    private String decisionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_by_user_id")
    private User scheduledBy;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TripRequest() {
    }

    public TripRequest(
            User requester,
            Instant departureAt,
            Instant expectedReturnAt,
            String origin,
            String destination,
            String purpose,
            int passengerCount,
            Vehicle requestedVehicle
    ) {
        if (requester == null) {
            throw new InvalidRequestException("O solicitante é obrigatório");
        }
        if (!requester.isActive()) {
            throw new ConflictException("Usuário inativo não pode solicitar viagem");
        }
        this.requester = requester;
        applyTripDetails(departureAt, expectedReturnAt, origin, destination, purpose, passengerCount, requestedVehicle);
        this.status = TripRequestStatus.PENDING;
    }

    public void updatePending(
            Instant departureAt,
            Instant expectedReturnAt,
            String origin,
            String destination,
            String purpose,
            int passengerCount,
            Vehicle requestedVehicle
    ) {
        if (this.status != TripRequestStatus.PENDING) {
            throw new ConflictException("Somente solicitações pendentes podem ser editadas");
        }
        applyTripDetails(departureAt, expectedReturnAt, origin, destination, purpose, passengerCount, requestedVehicle);
    }

    public void cancelByRequester(Instant canceledAt) {
        if (this.status != TripRequestStatus.PENDING) {
            throw new ConflictException("Somente solicitações pendentes podem ser canceladas pelo solicitante");
        }
        this.status = TripRequestStatus.CANCELED;
        this.canceledAt = Objects.requireNonNull(canceledAt, "Data de cancelamento é obrigatória");
    }

    public void approve(User decidedBy, Vehicle preAssignedVehicle, String decisionNotes, Instant decidedAt) {
        if (this.status != TripRequestStatus.PENDING) {
            throw new ConflictException("Somente solicitações pendentes podem ser aprovadas");
        }
        if (decidedBy == null) {
            throw new InvalidRequestException("O responsável pela decisão é obrigatório");
        }
        if (preAssignedVehicle != null) {
            validateVehicleEligibility(preAssignedVehicle, this.passengerCount);
        }
        this.status = TripRequestStatus.APPROVED;
        this.decidedBy = decidedBy;
        this.decidedAt = Objects.requireNonNull(decidedAt, "Data da decisão é obrigatória");
        this.assignedVehicle = preAssignedVehicle;
        this.decisionNotes = normalizeOptionalText(decisionNotes, "Observações da decisão", 500);
        this.rejectionReason = null;
    }

    public void reject(User decidedBy, String rejectionReason, Instant decidedAt) {
        if (this.status != TripRequestStatus.PENDING) {
            throw new ConflictException("Somente solicitações pendentes podem ser recusadas");
        }
        if (decidedBy == null) {
            throw new InvalidRequestException("O responsável pela decisão é obrigatório");
        }
        this.rejectionReason = normalizeRequiredText(
                rejectionReason,
                "A justificativa é obrigatória na recusa da solicitação",
                500
        );
        this.status = TripRequestStatus.REJECTED;
        this.decidedBy = decidedBy;
        this.decidedAt = Objects.requireNonNull(decidedAt, "Data da decisão é obrigatória");
        this.assignedVehicle = null;
        this.assignedDriver = null;
    }

    public void schedule(User scheduledBy, Vehicle vehicle, Driver driver, Instant scheduledAt) {
        if (this.status != TripRequestStatus.APPROVED) {
            throw new ConflictException("Somente solicitações aprovadas podem ser agendadas");
        }
        if (scheduledBy == null) {
            throw new InvalidRequestException("O responsável pelo agendamento é obrigatório");
        }
        if (vehicle == null) {
            throw new InvalidRequestException("O veículo é obrigatório para agendar a viagem");
        }
        if (driver == null) {
            throw new InvalidRequestException("O motorista é obrigatório para agendar a viagem");
        }
        validateVehicleEligibility(vehicle, this.passengerCount);
        if (!driver.isActive() || !driver.getUser().isActive()) {
            throw new ConflictException("O motorista selecionado está inativo");
        }
        this.assignedVehicle = vehicle;
        this.assignedDriver = driver;
        this.scheduledBy = scheduledBy;
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "Data do agendamento é obrigatória");
        this.status = TripRequestStatus.SCHEDULED;
    }

    public boolean overlapsWith(Instant start, Instant end) {
        return this.departureAt.isBefore(end) && this.expectedReturnAt.isAfter(start);
    }

    private void applyTripDetails(
            Instant departureAt,
            Instant expectedReturnAt,
            String origin,
            String destination,
            String purpose,
            int passengerCount,
            Vehicle requestedVehicle
    ) {
        if (departureAt == null || expectedReturnAt == null) {
            throw new InvalidRequestException("Data/hora de saída e retorno previsto são obrigatórias");
        }
        if (!expectedReturnAt.isAfter(departureAt)) {
            throw new InvalidRequestException("A data/hora prevista de retorno deve ser posterior à saída");
        }
        if (passengerCount < 1) {
            throw new InvalidRequestException("O número de passageiros deve ser pelo menos 1");
        }
        String normalizedOrigin = normalizeRequiredText(origin, "A origem é obrigatória", 200);
        String normalizedDestination = normalizeRequiredText(destination, "O destino é obrigatório", 200);
        String normalizedPurpose = normalizeRequiredText(purpose, "A finalidade é obrigatória", 500);

        if (requestedVehicle != null) {
            validateVehicleEligibility(requestedVehicle, passengerCount);
        }

        this.departureAt = departureAt;
        this.expectedReturnAt = expectedReturnAt;
        this.origin = normalizedOrigin;
        this.destination = normalizedDestination;
        this.purpose = normalizedPurpose;
        this.passengerCount = passengerCount;
        this.requestedVehicle = requestedVehicle;
    }

    private static void validateVehicleEligibility(Vehicle vehicle, int passengerCount) {
        if (!vehicle.isActive()) {
            throw new ConflictException("O veículo selecionado está inativo");
        }
        if (vehicle.isInMaintenance()) {
            throw new ConflictException("O veículo selecionado está em manutenção");
        }
        if (!vehicle.supportsPassengers(passengerCount)) {
            throw new InvalidRequestException(
                    "O veículo selecionado comporta no máximo "
                            + vehicle.getAvailablePassengerSeats()
                            + " passageiro(s), insuficiente para "
                            + passengerCount
                            + " passageiro(s)"
            );
        }
    }

    private static String normalizeRequiredText(String rawValue, String emptyMessage, int maxLength) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidRequestException(emptyMessage);
        }
        String normalized = rawValue.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidRequestException("Campo excede o tamanho máximo de " + maxLength + " caracteres");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String rawValue, String fieldLabel, int maxLength) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidRequestException(fieldLabel + " excede o tamanho máximo de " + maxLength + " caracteres");
        }
        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public User getRequester() {
        return requester;
    }

    public Instant getDepartureAt() {
        return departureAt;
    }

    public Instant getExpectedReturnAt() {
        return expectedReturnAt;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getPurpose() {
        return purpose;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public Vehicle getRequestedVehicle() {
        return requestedVehicle;
    }

    public TripRequestStatus getStatus() {
        return status;
    }

    public Vehicle getAssignedVehicle() {
        return assignedVehicle;
    }

    public Driver getAssignedDriver() {
        return assignedDriver;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getDecisionNotes() {
        return decisionNotes;
    }

    public User getScheduledBy() {
        return scheduledBy;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
