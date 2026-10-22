
package br.com.gestaofrota.application.services;

import br.com.gestaofrota.api.v1.dtos.requests.CreateVehicleRequest;
import br.com.gestaofrota.api.v1.dtos.requests.UpdateVehicleStatus;
import br.com.gestaofrota.api.v1.dtos.responses.VehicleResponse;
import br.com.gestaofrota.domain.enums.VehicleStatus;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.SectorRepository;
import br.com.gestaofrota.infra.repositories.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final SectorRepository sectorRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            SectorRepository sectorRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.sectorRepository = sectorRepository;
    }

    @Transactional
    public VehicleResponse create(CreateVehicleRequest request) {

        // Busca o setor informado na requisição
        Sector sector = sectorRepository.findById(request.sectorId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Setor não encontrado: " + request.sectorId()
                        )
                );

        // Cria a entidade e preenche os dados
        Vehicle vehicle = new Vehicle();
        vehicle.setSector(sector);
        vehicle.setPlate(request.plate());
        vehicle.setModel(request.model());
        vehicle.setCapacity(request.capacity());
        vehicle.setCurrentMileage(request.currentMileage());
        vehicle.setStatus(request.status());

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

        vehicle.setStatus(request.status());

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
}
