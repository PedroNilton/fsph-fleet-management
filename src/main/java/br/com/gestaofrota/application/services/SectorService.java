package br.com.gestaofrota.application.services;

import br.com.gestaofrota.application.exceptions.ConflictException;
import br.com.gestaofrota.application.exceptions.ResourceNotFoundException;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.infra.repositories.SectorRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final UserRepository userRepository;

    public SectorService(SectorRepository sectorRepository, UserRepository userRepository) {
        this.sectorRepository = sectorRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Sector> list() {
        return sectorRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional(readOnly = true)
    public Sector find(UUID id) {
        return sectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado"));
    }

    @Transactional
    public Sector create(String name) {
        String normalizedName = normalizeName(name);
        if (sectorRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Já existe um setor com este nome");
        }
        return sectorRepository.save(new Sector(normalizedName));
    }

    @Transactional
    public Sector update(UUID id, String name) {
        Sector sector = find(id);
        String normalizedName = normalizeName(name);
        if (sectorRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new ConflictException("Já existe um setor com este nome");
        }
        sector.rename(normalizedName);
        return sector;
    }

    @Transactional
    public void delete(UUID id) {
        Sector sector = find(id);
        if (userRepository.existsBySectorId(id)) {
            throw new ConflictException("O setor possui usuários vinculados");
        }
        sectorRepository.delete(sector);
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}
