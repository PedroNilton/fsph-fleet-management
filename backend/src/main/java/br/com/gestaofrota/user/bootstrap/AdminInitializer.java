package br.com.gestaofrota.user.bootstrap;

import br.com.gestaofrota.sector.Sector;
import br.com.gestaofrota.sector.SectorRepository;
import br.com.gestaofrota.user.User;
import br.com.gestaofrota.user.UserRepository;
import br.com.gestaofrota.user.UserRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;

    public AdminInitializer(UserRepository userRepository, SectorRepository sectorRepository, PasswordEncoder passwordEncoder, AdminProperties properties) {
        this.userRepository = userRepository;
        this.sectorRepository = sectorRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedEmail = properties.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        String sectorName = properties.sectorName().trim();
        Sector administrativeSector = sectorRepository.findByNameIgnoreCase(sectorName)
                .orElseGet(() -> sectorRepository.save(new Sector(sectorName)));

        User administrator = new User(
            administrativeSector,
            properties.name().trim(),
            normalizedEmail,
            passwordEncoder.encode(properties.password()),
            UserRole.ADMIN
        );

        userRepository.save(administrator);
    }
}
