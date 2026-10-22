package br.com.gestaofrota.application.services;

import br.com.gestaofrota.domain.enums.RecordStatus;
import br.com.gestaofrota.domain.enums.VehicleDocumentType;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import br.com.gestaofrota.domain.exceptions.ResourceNotFoundException;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.domain.models.VehicleDocument;
import br.com.gestaofrota.infra.repositories.VehicleDocumentRepository;
import br.com.gestaofrota.infra.repositories.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class VehicleDocumentService {

    private final VehicleRepository vehicles;
    private final VehicleDocumentRepository documents;

    public VehicleDocumentService(VehicleRepository vehicles, VehicleDocumentRepository documents) {
        this.vehicles = vehicles;
        this.documents = documents;
    }

    @Transactional(readOnly = true)
    public List<VehicleDocument> list(UUID vehicleId) {
        ensureVehicleExists(vehicleId);

        return documents.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
    }

    @Transactional(readOnly = true)
    public VehicleDocument find(UUID vehicleId, UUID documentId) {
        return documents.findByIdAndVehicleId(documentId, vehicleId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Documento do veículo", documentId)
                );
    }

    @Transactional
    public VehicleDocument register(UUID vehicleId, VehicleDocumentType type, String number, LocalDate issuedOn, LocalDate expiresOn) {
        if (issuedOn != null && issuedOn.isAfter(expiresOn)) {
            throw new InvalidRequestException("Emissão não pode ser posterior à validade");
        }

        Vehicle vehicle = vehicles.lockById(vehicleId).orElseThrow(
                () -> new ResourceNotFoundException("Veículo", vehicleId)
        );

        if (!vehicle.isActive()) {
            throw new ConflictException("Não é possível cadastrar documento para veículo inativo");
        }

        documents.findByVehicleIdAndTypeAndStatus(vehicleId, type, RecordStatus.ACTIVE)
                .ifPresent(current -> {
                    current.replaced();
                    documents.saveAndFlush(current);
                });

        return documents.save(new VehicleDocument(
                vehicle,
                type,
                number == null || number.isBlank() ? null : number.trim(), issuedOn, expiresOn
            )
        );
    }

    private void ensureVehicleExists(UUID id) {
        if (!vehicles.existsById(id)) {
            throw new ResourceNotFoundException("Veículo", id);
        }
    }
}