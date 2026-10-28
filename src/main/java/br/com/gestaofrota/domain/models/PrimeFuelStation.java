package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "prime_fuel_stations")
public class PrimeFuelStation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PrimeFuelStation() {
    }

    public PrimeFuelStation(String name, String cnpj, String city, String state, String address) {
        applyFields(name, cnpj, city, state, address);
    }

    public void update(String name, String cnpj, String city, String state, String address) {
        applyFields(name, cnpj, city, state, address);
    }

    public void deactivate() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }

    public static String normalizeCnpj(String rawCnpj) {
        if (rawCnpj == null || rawCnpj.isBlank()) {
            throw new InvalidRequestException("CNPJ do posto credenciado é obrigatório");
        }
        String digits = rawCnpj.replaceAll("[./\\-\\s]", "");
        if (!digits.matches("[0-9]{14}") || "00000000000000".equals(digits)) {
            throw new InvalidRequestException("CNPJ do posto credenciado deve conter 14 dígitos numéricos válidos");
        }
        return digits;
    }

    private void applyFields(String name, String cnpj, String city, String state, String address) {
        this.name = normalizeRequired(name, "Nome do posto credenciado é obrigatório");
        this.cnpj = normalizeCnpj(cnpj);
        this.city = normalizeRequired(city, "Município do posto credenciado é obrigatório");
        this.state = normalizeState(state);
        this.address = normalizeRequired(address, "Endereço do posto credenciado é obrigatório");
    }

    private static String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(message);
        }
        return value.trim();
    }

    private static String normalizeState(String state) {
        if (state == null || state.isBlank()) {
            throw new InvalidRequestException("UF do posto credenciado é obrigatória");
        }
        String normalized = state.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{2}")) {
            throw new InvalidRequestException("UF deve conter exatamente 2 letras");
        }
        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
