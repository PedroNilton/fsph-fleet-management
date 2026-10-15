package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.UserRole;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected User() {
    }

    public User(Sector sector, String name, String email, String password, UserRole role) {
        this.sector = sector;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public Sector getSector() {
        return sector;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void update(Sector sector, String name, String email, UserRole role, boolean active) {
        this.sector = sector;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void deactivate() {
        this.active = false;
    }
}
