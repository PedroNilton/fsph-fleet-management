package br.com.gestaofrota.infra.repositories;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.domain.enums.UserRole;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

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

    @EntityGraph(attributePaths = "sector")
    Optional<User> findByEmailIgnoreCaseAndActiveTrue(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsBySectorId(UUID sectorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.role = :role and user.active = true order by user.id")
    List<User> lockActiveByRole(@Param("role") UserRole role);
}
