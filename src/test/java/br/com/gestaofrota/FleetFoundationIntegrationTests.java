package br.com.gestaofrota;

import br.com.gestaofrota.api.v1.dtos.responses.DriverLicenseResponse;
import br.com.gestaofrota.api.v1.dtos.responses.VehicleDocumentResponse;
import br.com.gestaofrota.application.services.DriverLicenseService;
import br.com.gestaofrota.application.services.DriverService;
import br.com.gestaofrota.application.services.VehicleDocumentService;
import br.com.gestaofrota.application.services.VehicleService;
import br.com.gestaofrota.domain.enums.RecordStatus;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.enums.VehicleDocumentType;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import br.com.gestaofrota.domain.exceptions.ResourceNotFoundException;
import br.com.gestaofrota.domain.models.Driver;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.DriverLicenseRepository;
import br.com.gestaofrota.infra.repositories.SectorRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import br.com.gestaofrota.infra.repositories.VehicleDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class FleetFoundationIntegrationTests extends PostgresIntegrationTest {
    @Autowired private SectorRepository sectors;
    @Autowired private UserRepository users;
    @Autowired private VehicleService vehicles;
    @Autowired private VehicleDocumentService documents;
    @Autowired private VehicleDocumentRepository documentRepository;
    @Autowired private DriverService drivers;
    @Autowired private DriverLicenseService licenses;
    @Autowired private DriverLicenseRepository licenseRepository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void migratesNewTablesAndPersistsVehicleWithSector() {
        Sector sector = sector();
        Vehicle vehicle = vehicles.create(sector.getId(), "abc-1234", "Van", 8, "Transporte", 2500);

        Vehicle found = vehicles.find(vehicle.getId());
        assertThat(found.getPlate()).isEqualTo("ABC1234");
        assertThat(found.getSector().getId()).isEqualTo(sector.getId());
        assertThat(found.getCurrentOdometerKm()).isEqualTo(2500);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success AND version IN ('6','7','8','9','10')",
                Integer.class)).isEqualTo(5);

        assertThatThrownBy(() -> vehicles.update(vehicle.getId(), sector.getId(), "ABC1234", "Van", 8,
                "Transporte", 2499)).isInstanceOf(ConflictException.class);
    }

    @Test
    void databasePreventsPlateDuplicateIgnoringCase() {
        Vehicle vehicle = vehicles.create(null, plate(), "Van", 8, "Transporte", 0);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO vehicles (plate, model, capacity, usage_type, current_odometer_km)
                VALUES (?, 'Outra van', 8, 'Transporte', 0)
                """, vehicle.getPlate().toLowerCase()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void replacesVehicleDocumentWithoutLosingHistory() {
        Vehicle vehicle = vehicles.create(null, plate(), "Van", 8, "Transporte", 0);
        var first = documents.register(vehicle.getId(), VehicleDocumentType.LICENSING, "A1",
                LocalDate.now().minusYears(1), LocalDate.now().minusDays(1));
        var second = documents.register(vehicle.getId(), VehicleDocumentType.LICENSING, "A2",
                LocalDate.now(), LocalDate.now().plusDays(20));

        assertThat(documents.list(vehicle.getId())).hasSize(2)
                .extracting(document -> document.getStatus())
                .containsExactlyInAnyOrder(RecordStatus.ACTIVE, RecordStatus.REPLACED);
        assertThat(documentRepository.findById(first.getId()).orElseThrow().getStatus())
                .isEqualTo(RecordStatus.REPLACED);
        assertThat(documents.find(vehicle.getId(), second.getId()).getId()).isEqualTo(second.getId());
        Vehicle anotherVehicle = vehicles.create(null, plate(), "Carro", 4, "Transporte", 0);
        assertThatThrownBy(() -> documents.find(anotherVehicle.getId(), second.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        VehicleDocumentResponse response = VehicleDocumentResponse.from(second);
        assertThat(response.expiringWithin30Days()).isTrue();
        assertThat(response.expired()).isFalse();
        assertThatThrownBy(() -> documents.register(vehicle.getId(), VehicleDocumentType.IPVA, null,
                LocalDate.now().plusDays(2), LocalDate.now()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void databasePreventsTwoCurrentDocumentsOfSameType() {
        Vehicle vehicle = vehicles.create(null, plate(), "Van", 8, "Transporte", 0);
        documents.register(vehicle.getId(), VehicleDocumentType.INSURANCE, null, null, LocalDate.now().plusYears(1));

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO vehicle_documents (vehicle_id, type, expires_on, status)
                VALUES (?, 'INSURANCE', ?, 'ACTIVE')
                """, vehicle.getId(), LocalDate.now().plusYears(2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void linksDriverToUserAndKeepsLicenseHistory() {
        User user = motorist();
        Driver driver = drivers.create(user.getId());
        assertThat(drivers.find(driver.getId()).getUser().getId()).isEqualTo(user.getId());

        var old = licenses.register(driver.getId(), "12345678900", "B",
                LocalDate.now().minusYears(4), LocalDate.now().minusDays(1));
        var current = licenses.register(driver.getId(), "12345678900", "D",
                LocalDate.now(), LocalDate.now().plusDays(15));

        assertThat(licenses.list(driver.getId())).hasSize(2);
        assertThat(licenseRepository.findById(old.getId()).orElseThrow().getStatus())
                .isEqualTo(RecordStatus.REPLACED);
        assertThat(licenses.find(driver.getId(), current.getId()).getId()).isEqualTo(current.getId());
        Driver anotherDriver = drivers.create(motorist().getId());
        assertThatThrownBy(() -> licenses.find(anotherDriver.getId(), current.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        DriverLicenseResponse response = DriverLicenseResponse.from(current);
        assertThat(response.expiringWithin30Days()).isTrue();
        assertThat(response.status()).isEqualTo(RecordStatus.ACTIVE);
        assertThatThrownBy(() -> drivers.create(user.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsNonMotoristAndDuplicateCurrentLicenseNumber() {
        Sector sector = sector();
        User employee = users.save(new User(sector, "Empregado", email(), "hash", UserRole.EMPLOYEE));
        assertThatThrownBy(() -> drivers.create(employee.getId())).isInstanceOf(ConflictException.class);

        Driver one = drivers.create(motorist().getId());
        Driver two = drivers.create(motorist().getId());
        licenses.register(one.getId(), "CNH-001", "B", LocalDate.now(), LocalDate.now().plusYears(1));
        assertThatThrownBy(() -> licenses.register(two.getId(), "cnh-001", "B",
                LocalDate.now(), LocalDate.now().plusYears(1))).isInstanceOf(ConflictException.class);
    }

    @Test
    void concurrentDocumentReplacementsLeaveOneCurrentVersion() {
        Vehicle vehicle = vehicles.create(null, plate(), "Van", 8, "Transporte", 0);
        documents.register(vehicle.getId(), VehicleDocumentType.IPVA, "I0", null, LocalDate.now().plusYears(1));

        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> documents.register(vehicle.getId(),
                VehicleDocumentType.IPVA, "I1", null, LocalDate.now().plusYears(1)));
        CompletableFuture<Void> second = CompletableFuture.runAsync(() -> documents.register(vehicle.getId(),
                VehicleDocumentType.IPVA, "I2", null, LocalDate.now().plusYears(1)));
        CompletableFuture.allOf(first, second).orTimeout(30, TimeUnit.SECONDS).join();

        assertThat(documents.list(vehicle.getId())).hasSize(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM vehicle_documents
                WHERE vehicle_id = ? AND type = 'IPVA' AND status = 'ACTIVE'
                """, Integer.class, vehicle.getId())).isEqualTo(1);
    }

    private Sector sector() { return sectors.save(new Sector("Setor " + UUID.randomUUID())); }
    private User motorist() {
        return users.save(new User(sector(), "Motorista", email(), "hash", UserRole.MOTORISTA));
    }
    private String email() { return "user-" + UUID.randomUUID() + "@example.com"; }
    private String plate() { return UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase(); }
}
