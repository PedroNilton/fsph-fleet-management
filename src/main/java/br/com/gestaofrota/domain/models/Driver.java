package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.DriverStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "drivers")
public class Driver{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 20)
    private String license_number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 150)
    private DriverStatus status;

    public Driver(UUID id, User user, String license_number, DriverStatus status) {
        this.id = id;
        this.user = user;
        this.license_number = license_number;
        this.status = status;
    }

    public Driver() {
    }

    public Driver(User user, String licenseNumber, DriverStatus status) {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLicense_number() {
        return license_number;
    }

    public void setLicense_number(String license_number) {
        this.license_number = license_number;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }
}
