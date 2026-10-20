package br.com.gestaofrota.infra.repositories;

import br.com.gestaofrota.domain.enums.VehicleOwner;
import br.com.gestaofrota.domain.enums.VehicleRole;
import br.com.gestaofrota.domain.enums.VehicleStatus;
import br.com.gestaofrota.domain.models.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByPlate(String plate); //achar por placa

    List<Vehicle> findByStatus(VehicleStatus status); // Listar veiculos por status

    List<Vehicle> findByOwner(VehicleOwner owner); // Listar por propiedade
    List<Vehicle> findByOwnerAndStatus(VehicleOwner owner, VehicleStatus status); // Listar veiculos por status e propiedade

    List<Vehicle> findByRole(VehicleRole role); // listar por tipo de veiculo
    List<Vehicle> findByAvailableSeats(int availableSeats); // listar por vagas disponiveis em cada carro



}
