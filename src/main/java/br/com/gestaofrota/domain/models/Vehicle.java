package br.com.gestaofrota.domain.models;


import br.com.gestaofrota.domain.enums.VehicleStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false, length = 150)
    private String plate;

    @Column(nullable = false, length = 150)
    private String model;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int current_mileage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 150)
    private VehicleStatus status;

    public Vehicle() {
    }

    public Vehicle(UUID id, Sector sector, String plate, String model, int capacity, int current_mileage, VehicleStatus status) {
        this.id = id;
        this.sector = sector;
        this.plate = plate;
        this.model = model;
        this.capacity = capacity;
        this.current_mileage = current_mileage;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCurrent_mileage() {
        return current_mileage;
    }

    public void setCurrent_mileage(int current_mileage) {
        this.current_mileage = current_mileage;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }
}
