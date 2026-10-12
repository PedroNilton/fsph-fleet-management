package br.com.gestaofrota.modules.user.infra.repository;

import br.com.gestaofrota.modules.user.infra.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCaseAndActiveTrue(String email);

    boolean existsByEmailIgnoreCase(String email);
}
