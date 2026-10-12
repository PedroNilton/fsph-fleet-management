package br.com.gestaofrota.modules.sector.infra.repository;

import br.com.gestaofrota.modules.sector.infra.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SectorRepository extends JpaRepository<Sector, UUID> {
    Optional<Sector> findByNameIgnoreCase(String name);
}