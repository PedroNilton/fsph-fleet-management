package br.com.gestaofrota.application.services;

import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.SvoShiftStatus;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.InvalidRequestException;
import br.com.gestaofrota.domain.models.SvoDutyShift;
import br.com.gestaofrota.domain.models.SvoTeam;
import br.com.gestaofrota.domain.models.Vehicle;
import br.com.gestaofrota.infra.repositories.SvoDutyShiftRepository;
import br.com.gestaofrota.infra.repositories.SvoTeamRepository;
import br.com.gestaofrota.infra.repositories.VehicleRepository;
import br.com.gestaofrota.infra.security.models.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SvoDutyShiftServiceTests {

    @Mock
    private SvoDutyShiftRepository shiftRepository;

    @Mock
    private SvoTeamRepository teamRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private MaintenanceAvailabilityGuard maintenanceGuard;

    @Mock
    private AuditLogService auditLogService;

    private SvoDutyShiftService shiftService;

    private AuthenticatedUser actor;
    private SvoTeam team;

    @BeforeEach
    void setUp() {
        shiftService = new SvoDutyShiftService(
                shiftRepository,
                teamRepository,
                vehicleRepository,
                maintenanceGuard,
                auditLogService
        );

        actor = new AuthenticatedUser(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Gestor Frota",
                "gestor@fsph.se.gov.br",
                "secret",
                UserRole.GESTOR_FROTA,
                true,
                Set.of(Permission.SVO_SHIFTS_READ, Permission.SVO_SHIFTS_WRITE)
        );

        team = new SvoTeam("Equipe SVO 01", "Equipe Diurna");
        ReflectionTestUtils.setField(team, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("Should create duty shift successfully")
    void createDutyShiftSuccessfully() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(12, ChronoUnit.HOURS);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(shiftRepository.findTeamShiftsOverlapping(eq(team.getId()), any(), any(), any()))
                .thenReturn(List.of());
        when(shiftRepository.save(any(SvoDutyShift.class))).thenAnswer(i -> {
            SvoDutyShift shift = i.getArgument(0);
            ReflectionTestUtils.setField(shift, "id", UUID.randomUUID());
            return shift;
        });

        SvoDutyShift result = shiftService.create(team.getId(), null, start, end, "DIURNO", "Sem obs", actor);

        assertThat(result).isNotNull();
        assertThat(result.getTeam()).isEqualTo(team);
        assertThat(result.getStartAt()).isEqualTo(start);
        assertThat(result.getEndAt()).isEqualTo(end);
        assertThat(result.getStatus()).isEqualTo(SvoShiftStatus.SCHEDULED);

        verify(auditLogService).record(eq(actor), eq("SVO_SHIFT_CREATED"), eq("SVO_SHIFT"), any(), any());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when startAt is after endAt")
    void throwWhenPeriodInvalid() {
        Instant start = Instant.now().plus(10, ChronoUnit.HOURS);
        Instant end = start.minus(2, ChronoUnit.HOURS);

        assertThatThrownBy(() -> shiftService.create(team.getId(), null, start, end, "DIURNO", null, actor))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("A data/hora de término deve ser posterior ao início");
    }

    @Test
    @DisplayName("Should throw ConflictException when team is inactive")
    void throwWhenTeamInactive() {
        team.deactivate();
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(12, ChronoUnit.HOURS);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> shiftService.create(team.getId(), null, start, end, "DIURNO", null, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("inativa não pode ser escalada");
    }

    @Test
    @DisplayName("Should throw ConflictException when team already has overlapping shift")
    void throwWhenTeamHasOverlappingShift() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(12, ChronoUnit.HOURS);

        SvoDutyShift existing = new SvoDutyShift(team, null, start, end, "DIURNO", null);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(shiftRepository.findTeamShiftsOverlapping(eq(team.getId()), any(), any(), any()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> shiftService.create(team.getId(), null, start, end, "DIURNO", null, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já possui plantão agendado para o período informado");
    }

    @Test
    @DisplayName("Should cancel duty shift successfully")
    void cancelShiftSuccessfully() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(12, ChronoUnit.HOURS);
        SvoDutyShift shift = new SvoDutyShift(team, null, start, end, "DIURNO", null);
        UUID shiftId = UUID.randomUUID();
        ReflectionTestUtils.setField(shift, "id", shiftId);

        when(shiftRepository.findWithDetailsById(shiftId)).thenReturn(Optional.of(shift));
        when(shiftRepository.save(shift)).thenReturn(shift);

        SvoDutyShift canceled = shiftService.cancel(shiftId, actor);

        assertThat(canceled.getStatus()).isEqualTo(SvoShiftStatus.CANCELED);
        verify(auditLogService).record(eq(actor), eq("SVO_SHIFT_CANCELED"), eq("SVO_SHIFT"), eq(shiftId.toString()), any());
    }

    @Test
    @DisplayName("Should return current shifts for now")
    void returnCurrentShifts() {
        Instant now = Instant.now();
        SvoDutyShift activeShift = new SvoDutyShift(team, null, now.minus(2, java.time.temporal.ChronoUnit.HOURS), now.plus(6, java.time.temporal.ChronoUnit.HOURS), "DIURNO", null);
        org.springframework.test.util.ReflectionTestUtils.setField(activeShift, "status", SvoShiftStatus.ACTIVE);

        when(shiftRepository.findCurrentShifts(any(), any())).thenReturn(List.of(activeShift));

        List<SvoDutyShift> current = shiftService.getCurrentShifts(now);

        assertThat(current).hasSize(1);
        assertThat(current.get(0)).isEqualTo(activeShift);
    }
}
