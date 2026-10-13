package br.com.gestaofrota.modules.user.bootstrap;

import br.com.gestaofrota.modules.sector.infra.entity.Sector;
import br.com.gestaofrota.modules.sector.infra.repository.SectorRepository;
import br.com.gestaofrota.modules.user.domain.UserRole;
import br.com.gestaofrota.modules.user.infra.entity.User;
import br.com.gestaofrota.modules.user.infra.repository.UserRepository;
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
