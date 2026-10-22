package br.com.gestaofrota.infra.repositories;

import br.com.gestaofrota.domain.enums.RecordStatus;
import br.com.gestaofrota.domain.models.DriverLicense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverLicenseRepository extends JpaRepository<DriverLicense, UUID> {

    List<DriverLicense> findByDriverIdOrderByCreatedAtDesc(UUID driverId);

    Optional<DriverLicense> findByIdAndDriverId(UUID id, UUID driverId);

    Optional<DriverLicense> findByDriverIdAndStatus(UUID driverId, RecordStatus status);

    boolean existsByLicenseNumberIgnoreCaseAndStatusAndDriverIdNot(String licenseNumber, RecordStatus status, UUID driverId);
}