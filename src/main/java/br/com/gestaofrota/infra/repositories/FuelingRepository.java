package br.com.gestaofrota.infra.repositories;

import br.com.gestaofrota.domain.models.Fueling;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuelingRepository extends JpaRepository<Fueling, UUID> {

    @EntityGraph(attributePaths = {"vehicle", "vehicle.sector", "station", "registeredBy"})
    List<Fueling> findAllByOrderByFueledOnDescCreatedAtDesc();

    @EntityGraph(attributePaths = {"vehicle", "vehicle.sector", "station", "registeredBy"})
    List<Fueling> findByVehicleIdOrderByOdometerKmDescCreatedAtDesc(UUID vehicleId);

    @EntityGraph(attributePaths = {"vehicle", "vehicle.sector", "station", "registeredBy"})
    List<Fueling> findByRegisteredByIdOrderByFueledOnDescCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"vehicle", "vehicle.sector", "station", "registeredBy"})
    List<Fueling> findByOutOfPatternTrueOrderByFueledOnDescCreatedAtDesc();

    @EntityGraph(attributePaths = {"vehicle", "vehicle.sector", "station", "registeredBy"})
    Optional<Fueling> findWithDetailsById(UUID id);

    Optional<Fueling> findFirstByVehicleIdOrderByOdometerKmDescCreatedAtDesc(UUID vehicleId);
}

