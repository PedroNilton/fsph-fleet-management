package br.com.gestaofrota.infra.repositories;
import br.com.gestaofrota.domain.models.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SectorRepository extends JpaRepository<Sector, UUID> {
    Optional<Sector> findByNameIgnoreCase(String name);
}
