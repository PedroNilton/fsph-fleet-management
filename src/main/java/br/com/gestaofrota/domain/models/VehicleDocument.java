package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.RecordStatus;
import br.com.gestaofrota.domain.enums.VehicleDocumentType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicle_documents")
public class VehicleDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleDocumentType type;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "issued_on")
    private LocalDate issuedOn;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VehicleDocument() {
    }

    public VehicleDocument(Vehicle vehicle, VehicleDocumentType type, String documentNumber, LocalDate issuedOn, LocalDate expiresOn) {
        this.vehicle = vehicle;
        this.type = type;
        this.documentNumber = documentNumber;
        this.issuedOn = issuedOn;
        this.expiresOn = expiresOn;
    }

    public UUID getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public VehicleDocumentType getType() {
        return type;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void replaced() {
        status = RecordStatus.REPLACED;
    }
}