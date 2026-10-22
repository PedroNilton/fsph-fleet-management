package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.RecordStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "driver_licenses")
public class DriverLicense {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "license_number", nullable = false, length = 30)
    private String licenseNumber;

    @Column(nullable = false, length = 10)
    private String category;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DriverLicense() {
    }

    public DriverLicense(Driver driver, String licenseNumber, String category, LocalDate issuedOn, LocalDate expiresOn) {
        this.driver = driver;
        this.licenseNumber = licenseNumber;
        this.category = category;
        this.issuedOn = issuedOn;
        this.expiresOn = expiresOn;
    }

    public UUID getId() {
        return id;
    }

    public Driver getDriver() {
        return driver;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getCategory() {
        return category;
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
