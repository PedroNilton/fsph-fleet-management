package br.com.gestaofrota.infra.security.services;

import br.com.gestaofrota.api.v1.dtos.requests.CreateVehicleRequest;
import br.com.gestaofrota.api.v1.dtos.requests.UpdateVehicleStatus;
import br.com.gestaofrota.api.v1.dtos.responses.VehicleResponse;
import br.com.gestaofrota.domain.enums.VehicleOwner;
import br.com.gestaofrota.domain.enums.VehicleRole;
import br.com.gestaofrota.domain.enums.VehicleStatus;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public VehicleResponse create(CreateVehicleRequest request) {
        Vehicle vehicle = new Vehicle(
                null,
                request.plate(),
                request.model(),
                request.role(),
                request.owner(),
                request.status(),
                request.availableSeats()
        );

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return VehicleResponse.from(savedVehicle);
    }

    @Transactional
    public VehicleResponse updateStatus(
            UUID id,
            UpdateVehicleStatus request
    ) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Veículo não encontrado: " + id
                        )
                );

        vehicle.changeStatus(request.status());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);

        return VehicleResponse.from(updatedVehicle);
    }

    @Transactional(readOnly = true)
    public VehicleResponse findById(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Veículo não encontrado: " + id
                        )
                );

        return VehicleResponse.from(vehicle);
    }

    @Transactional(readOnly = true)
    public VehicleResponse findByPlate(String plate) {
        Vehicle vehicle = vehicleRepository.findByPlate(plate)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Veículo não encontrado com a placa: " + plate
                        )
                );

        return VehicleResponse.from(vehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findAll() {
        return vehicleRepository.findAll()
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findByStatus(VehicleStatus status) {
        return vehicleRepository.findByStatus(status)
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findByOwner(VehicleOwner owner) {
        return vehicleRepository.findByOwner(owner)
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findByOwnerAndStatus(
            VehicleOwner owner,
            VehicleStatus status
    ) {
        return vehicleRepository.findByOwnerAndStatus(owner, status)
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findByRole(VehicleRole role) {
        return vehicleRepository.findByRole(role)
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findByAvailableSeats(int availableSeats) {
        return vehicleRepository.findByAvailableSeats(availableSeats)
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }
}
