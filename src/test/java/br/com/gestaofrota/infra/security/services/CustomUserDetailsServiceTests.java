package br.com.gestaofrota.infra.security.services;

import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.infra.repositories.UserRepository;
import br.com.gestaofrota.domain.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTests {

    @Mock
    private UserRepository userRepository;

    @Test
    void loadsActiveUserWithRoleAsAuthority() {
        User administrator = new User(
                new Sector("Administrativo"),
                "Administrador",
                "admin@exemplo.com",
                "senha-codificada",
                UserRole.ADMIN
        );
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("admin@exemplo.com"))
                .thenReturn(Optional.of(administrator));

        UserDetails result = new CustomUserDetailsService(userRepository)
                .loadUserByUsername(" ADMIN@EXEMPLO.COM ");

        assertThat(result.getUsername()).isEqualTo("admin@exemplo.com");
        assertThat(result.getPassword()).isEqualTo("senha-codificada");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(userRepository).findByEmailIgnoreCaseAndActiveTrue("admin@exemplo.com");
    }

    @Test
    void rejectsMissingOrInactiveUserWithoutRevealingWhichCaseOccurred() {
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("inativo@exemplo.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CustomUserDetailsService(userRepository)
                .loadUserByUsername("inativo@exemplo.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Credenciais inválidas");
    }
}
