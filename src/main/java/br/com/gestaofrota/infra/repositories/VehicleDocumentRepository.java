package br.com.gestaofrota.infra.repositories;

import br.com.gestaofrota.domain.enums.RecordStatus;
import br.com.gestaofrota.domain.enums.VehicleDocumentType;
import br.com.gestaofrota.domain.models.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {
    List<VehicleDocument> findByVehicleIdOrderByCreatedAtDesc(UUID vehicleId);

    Optional<VehicleDocument> findByIdAndVehicleId(UUID id, UUID vehicleId);

    Optional<VehicleDocument> findByVehicleIdAndTypeAndStatus(UUID vehicleId, VehicleDocumentType type, RecordStatus status);
}