package br.com.gestaofrota.infra.repositories;

import br.com.gestaofrota.domain.models.PrimeFuelStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrimeFuelStationRepository extends JpaRepository<PrimeFuelStation, UUID> {

    List<PrimeFuelStation> findAllByOrderByNameAsc();

    List<PrimeFuelStation> findByActiveTrueOrderByNameAsc();

    Optional<PrimeFuelStation> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, UUID id);
}

