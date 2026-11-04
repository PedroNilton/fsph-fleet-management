package br.com.gestaofrota.infra.repositories;

import br.com.gestaofrota.domain.enums.TripRequestStatus;
import br.com.gestaofrota.domain.models.TripRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRequestRepository extends JpaRepository<TripRequest, UUID> {

    @EntityGraph(attributePaths = {
            "requester", "requester.sector",
            "requestedVehicle", "requestedVehicle.sector",
            "assignedVehicle", "assignedVehicle.sector",
            "assignedDriver", "assignedDriver.user",
            "decidedBy", "scheduledBy"
    })
    Optional<TripRequest> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {
            "requester", "requester.sector",
            "requestedVehicle", "requestedVehicle.sector",
            "assignedVehicle", "assignedVehicle.sector",
            "assignedDriver", "assignedDriver.user",
            "decidedBy", "scheduledBy"
    })
    List<TripRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    @EntityGraph(attributePaths = {
            "requester", "requester.sector",
            "requestedVehicle", "requestedVehicle.sector",
            "assignedVehicle", "assignedVehicle.sector",
            "assignedDriver", "assignedDriver.user",
            "decidedBy", "scheduledBy"
    })
    List<TripRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(UUID requesterId, TripRequestStatus status);

    @EntityGraph(attributePaths = {
            "requester", "requester.sector",
            "requestedVehicle", "requestedVehicle.sector",
            "assignedVehicle", "assignedVehicle.sector",
            "assignedDriver", "assignedDriver.user",
            "decidedBy", "scheduledBy"
    })
    List<TripRequest> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {
            "requester", "requester.sector",
            "requestedVehicle", "requestedVehicle.sector",
            "assignedVehicle", "assignedVehicle.sector",
            "assignedDriver", "assignedDriver.user",
            "decidedBy", "scheduledBy"
    })
    List<TripRequest> findByStatusOrderByDepartureAtAsc(TripRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select tr from TripRequest tr
            join fetch tr.requester requester
            left join fetch requester.sector
            left join fetch tr.requestedVehicle requestedVehicle
            left join fetch requestedVehicle.sector
            left join fetch tr.assignedVehicle assignedVehicle
            left join fetch assignedVehicle.sector
            left join fetch tr.assignedDriver assignedDriver
            left join fetch assignedDriver.user
            left join fetch tr.decidedBy
            left join fetch tr.scheduledBy
            where tr.id = :id
            """)
    Optional<TripRequest> lockById(@Param("id") UUID id);

    @Query("""
            select tr from TripRequest tr
            where tr.assignedVehicle.id = :vehicleId
              and tr.status in :statuses
              and tr.departureAt < :end
              and tr.expectedReturnAt > :start
            """)
    List<TripRequest> findVehicleReservationsOverlapping(
            @Param("vehicleId") UUID vehicleId,
            @Param("statuses") Collection<TripRequestStatus> statuses,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
            select tr from TripRequest tr
            where tr.assignedDriver.id = :driverId
              and tr.status in :statuses
              and tr.departureAt < :end
              and tr.expectedReturnAt > :start
            """)
    List<TripRequest> findDriverReservationsOverlapping(
            @Param("driverId") UUID driverId,
            @Param("statuses") Collection<TripRequestStatus> statuses,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @EntityGraph(attributePaths = {
            "requester", "requester.sector",
            "requestedVehicle", "requestedVehicle.sector",
            "assignedVehicle", "assignedVehicle.sector",
            "assignedDriver", "assignedDriver.user",
            "decidedBy", "scheduledBy"
    })
    @Query("""
            select tr from TripRequest tr
            where tr.status in :statuses
              and tr.departureAt < :end
              and tr.expectedReturnAt > :start
            order by tr.departureAt asc
            """)
    List<TripRequest> findActiveReservationsOverlapping(
            @Param("statuses") Collection<TripRequestStatus> statuses,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
