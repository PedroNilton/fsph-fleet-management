package br.com.gestaofrota;

import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.infra.repositories.SectorRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "POSTGRES_DB", matches = ".+")
class UserRolePersistenceIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void roundTripsEveryRoleThroughJsonJpaAndPostgres() throws Exception {
        Sector sector = sectorRepository.saveAndFlush(new Sector("Teste de perfis " + UUID.randomUUID()));

        for (UserRole role : UserRole.values()) {
            String json = objectMapper.writeValueAsString(role);
            assertThat(json).isEqualTo("\"" + role.name() + "\"");
            assertThat(objectMapper.readValue(json, UserRole.class)).isEqualTo(role);

            User saved = userRepository.saveAndFlush(new User(
                    sector,
                    "Usuário " + role.name(),
                    role.name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@example.com",
                    "password-hash",
                    role
            ));
            UUID userId = saved.getId();

            entityManager.clear();

            assertThat(userRepository.findById(userId)).get().extracting(User::getRole).isEqualTo(role);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT role FROM users WHERE id = ?",
                    String.class,
                    userId
            )).isEqualTo(role.name());
        }
    }

    @Test
    void schemaRoleLengthMatchesJpaMappingAndFitsEveryRole() throws NoSuchFieldException {
        int jpaLength = User.class.getDeclaredField("role").getAnnotation(Column.class).length();
        Integer databaseLength = jdbcTemplate.queryForObject("""
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'users'
                  AND column_name = 'role'
                """, Integer.class);
        int longestRole = Arrays.stream(UserRole.values())
                .map(Enum::name)
                .mapToInt(String::length)
                .max()
                .orElseThrow();

        assertThat(databaseLength).isEqualTo(jpaLength);
        assertThat(databaseLength).isGreaterThanOrEqualTo(longestRole);
    }

    @Test
    void databaseConstraintRejectsLegacyInstitutionalUserRole() {
        Sector sector = sectorRepository.saveAndFlush(new Sector("Teste de perfil legado " + UUID.randomUUID()));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO users (sector_id, name, email, password_hash, role)
                VALUES (?, ?, ?, ?, ?)
                """,
                sector.getId(),
                "Usuário legado",
                "legacy-" + UUID.randomUUID() + "@example.com",
                "password-hash",
                "USUARIO_INSTITUCIONAL"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
