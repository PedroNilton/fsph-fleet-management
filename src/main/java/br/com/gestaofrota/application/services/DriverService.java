package br.com.gestaofrota.application.services;

import br.com.gestaofrota.domain.enums.DriverStatus;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.infra.repositories.DriverRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DriverService {
    private final DriverRepository driverRepository;


    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Transactional
    public Driver create(User user, String license_number, DriverStatus status){
        Driver driver = new Driver(user,license_number,status);
        return driverRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public List<Driver> list() {
        return driverRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional
    public void delete(UUID id){
        Driver driver = findById(id);
        driverRepository.delete(driver);
    }

    @Transactional
    public Driver findById(UUID id){
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Motorista não encontrado"));
        return driver;
    }
    @Transactional
    public Driver update(UUID id, User user, String license_number, DriverStatus status){
        Driver driver = findById(id);

        driver.setUser(user);
        driver.setLicense_number(license_number);
        driver.setStatus(status);
        return driverRepository.save(driver);
    }







}
