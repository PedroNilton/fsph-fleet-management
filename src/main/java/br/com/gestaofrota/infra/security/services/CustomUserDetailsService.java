package br.com.gestaofrota.infra.security.services;

import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.infra.repositories.UserRepository;
import br.com.gestaofrota.infra.security.models.AuthenticatedUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCaseAndActiveTrue(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        return new AuthenticatedUser(
                user.getId(),
                user.getSector().getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.isActive()
        );
    }
}
