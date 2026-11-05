package br.com.gestaofrota.application.services;

import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.ResourceNotFoundException;
import br.com.gestaofrota.domain.models.PrimeFuelStation;
import br.com.gestaofrota.infra.repositories.PrimeFuelStationRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PrimeFuelStationService {

    private final PrimeFuelStationRepository stations;

    public PrimeFuelStationService(PrimeFuelStationRepository stations) {
        this.stations = stations;
    }

    @Transactional(readOnly = true)
    public List<PrimeFuelStation> list(Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return stations.findByActiveTrueOrderByNameAsc();
        }
        return stations.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public PrimeFuelStation find(UUID id) {
        return stations.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Posto credenciado Prime", id));
    }

    @Transactional
    public PrimeFuelStation create(String name, String cnpj, String city, String state, String address) {
        String normalizedCnpj = PrimeFuelStation.normalizeCnpj(cnpj);
        if (stations.existsByCnpj(normalizedCnpj)) {
            throw new ConflictException("Já existe um posto credenciado Prime com este CNPJ");
        }
        PrimeFuelStation station = new PrimeFuelStation(name, normalizedCnpj, city, state, address);
        try {
            return stations.saveAndFlush(station);
        } catch (DataIntegrityViolationException exception) {
            if (isCnpjUniqueViolation(exception)) {
                throw new ConflictException("Já existe um posto credenciado Prime com este CNPJ");
            }
            throw exception;
        }
    }

    @Transactional
    public PrimeFuelStation update(UUID id, String name, String cnpj, String city, String state, String address) {
        PrimeFuelStation station = find(id);
        String normalizedCnpj = PrimeFuelStation.normalizeCnpj(cnpj);
        if (stations.existsByCnpjAndIdNot(normalizedCnpj, id)) {
            throw new ConflictException("Já existe um posto credenciado Prime com este CNPJ");
        }
        station.update(name, normalizedCnpj, city, state, address);
        try {
            return stations.saveAndFlush(station);
        } catch (DataIntegrityViolationException exception) {
            if (isCnpjUniqueViolation(exception)) {
                throw new ConflictException("Já existe um posto credenciado Prime com este CNPJ");
            }
            throw exception;
        }
    }

    @Transactional
    public void deactivate(UUID id) {
        PrimeFuelStation station = find(id);
        station.deactivate();
    }

    @Transactional
    public void reactivate(UUID id) {
        PrimeFuelStation station = find(id);
        station.reactivate();
    }

    private boolean isCnpjUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return "ux_prime_fuel_stations_cnpj".equals(violation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}

