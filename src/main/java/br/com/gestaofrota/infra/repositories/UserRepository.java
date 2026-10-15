package br.com.gestaofrota.infra.repositories;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.enums.UserRole;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Override
    @EntityGraph(attributePaths = "sector")
    List<User> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "sector")
    Optional<User> findById(UUID id);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCaseAndActiveTrue(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsBySectorId(UUID sectorId);

    long countByRoleAndActiveTrue(UserRole role);
}
