package br.com.gestaofrota.application.services;

import br.com.gestaofrota.domain.enums.RecordStatus;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import br.com.gestaofrota.domain.exceptions.ResourceNotFoundException;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.DriverLicense;
import br.com.gestaofrota.infra.repositories.DriverLicenseRepository;
import br.com.gestaofrota.infra.repositories.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DriverLicenseService {

    private final DriverRepository drivers;
    private final DriverLicenseRepository licenses;

    public DriverLicenseService(DriverRepository drivers, DriverLicenseRepository licenses) {
        this.drivers = drivers;
        this.licenses = licenses;
    }

    @Transactional(readOnly = true)
    public List<DriverLicense> list(UUID driverId) {
        if (!drivers.existsById(driverId)) {
            throw new ResourceNotFoundException("Motorista", driverId);
        }

        return licenses.findByDriverIdOrderByCreatedAtDesc(driverId);
    }

    @Transactional(readOnly = true)
    public DriverLicense find(UUID driverId, UUID licenseId) {
        return licenses.findByIdAndDriverId(licenseId, driverId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("CNH", licenseId)
                );
    }

    @Transactional
    public DriverLicense register(UUID driverId, String number, String category, LocalDate issuedOn, LocalDate expiresOn) {
        if (issuedOn.isAfter(expiresOn)) {
            throw new InvalidRequestException("Emissão não pode ser posterior à validade");
        }

        Driver driver = drivers.lockById(driverId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Motorista", driverId)
                );

        if (!driver.isActive() || !driver.getUser().isActive()) {
            throw new ConflictException("Não é possível cadastrar CNH para motorista inativo");
        }

        String normalizedNumber = number.trim().toUpperCase(Locale.ROOT);

        if (licenses.existsByLicenseNumberIgnoreCaseAndStatusAndDriverIdNot(
                normalizedNumber, RecordStatus.ACTIVE, driverId)) {
            throw new ConflictException("Esta CNH vigente já pertence a outro motorista");
        }

        licenses.findByDriverIdAndStatus(driverId, RecordStatus.ACTIVE)
                .ifPresent(current -> {
                    current.replaced();
                    licenses.saveAndFlush(current);
                });

        return licenses.save(new DriverLicense(
                driver,
                normalizedNumber,
                category.trim().toUpperCase(Locale.ROOT),
                issuedOn,
                expiresOn)
        );
    }
}