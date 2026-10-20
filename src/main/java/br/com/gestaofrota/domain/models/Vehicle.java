package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.VehicleOwner;
import br.com.gestaofrota.domain.enums.VehicleRole;
import br.com.gestaofrota.domain.enums.VehicleStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String plate;

    @Column(nullable = false, length = 150)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleOwner owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 150)
    private VehicleStatus status;

    @Column(nullable = false)
    private int availableSeats;

    public Vehicle(UUID id, String plate, String model, VehicleRole role, VehicleOwner owner, VehicleStatus status, int availableSeats) {
        this.id = id;
        this.plate = plate;
        this.model = model;
        this.role = role;
        this.owner = owner;
        this.status = status;
        this.availableSeats = availableSeats;
    }

    public Vehicle() {
    }

    public void changeStatus(VehicleStatus status){
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public VehicleRole getRole() {
        return role;
    }

    public void setRole(VehicleRole role) {
        this.role = role;
    }

    public VehicleOwner getOwner() {
        return owner;
    }

    public void setOwner(VehicleOwner owner) {
        this.owner = owner;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }
}
