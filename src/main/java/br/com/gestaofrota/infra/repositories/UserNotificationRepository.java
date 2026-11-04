package br.com.gestaofrota.infra.repositories;

import br.com.gestaofrota.domain.models.UserNotification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    @EntityGraph(attributePaths = {"tripRequest"})
    List<UserNotification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    @EntityGraph(attributePaths = {"tripRequest"})
    List<UserNotification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(UUID recipientId);

    @EntityGraph(attributePaths = {"tripRequest"})
    Optional<UserNotification> findByIdAndRecipientId(UUID id, UUID recipientId);

    long countByRecipientIdAndReadFalse(UUID recipientId);
}
