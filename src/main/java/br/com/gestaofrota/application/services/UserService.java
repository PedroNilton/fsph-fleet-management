package br.com.gestaofrota.application.services;

import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.exceptions.ResourceNotFoundException;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.models.Sector;
import br.com.gestaofrota.domain.models.User;
import br.com.gestaofrota.infra.repositories.SectorRepository;
import br.com.gestaofrota.infra.repositories.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, SectorRepository sectorRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sectorRepository = sectorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional(readOnly = true)
    public User find(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }

    @Transactional
    public User create(UUID sectorId, String name, String email, String password, UserRole role) {
        Sector sector = findSector(sectorId);
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Já existe um usuário com este e-mail");
        }

        User user = new User(
                sector,
                name.trim(),
                normalizedEmail,
                passwordEncoder.encode(password),
                role
        );
        return userRepository.save(user);
    }

    @Transactional
    public User update(
            UUID id,
            UUID sectorId,
            String name,
            String email,
            String password,
            UserRole role,
            boolean active
    ) {
        User user = find(id);
        Sector sector = findSector(sectorId);
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new ConflictException("Já existe um usuário com este e-mail");
        }
        ensureAnActiveAdminRemains(user, role, active);

        user.update(sector, name.trim(), normalizedEmail, role, active);
        if (password != null && !password.isBlank()) {
            user.changePassword(passwordEncoder.encode(password));
        }
        return user;
    }

    @Transactional
    public void deactivate(UUID id) {
        User user = find(id);
        if (!user.isActive()) {
            return;
        }
        ensureAnActiveAdminRemains(user, user.getRole(), false);
        user.deactivate();
    }

    private Sector findSector(UUID id) {
        return sectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setor", id));
    }

    private void ensureAnActiveAdminRemains(User current, UserRole newRole, boolean active) {
        boolean removesActiveAdmin = current.getRole() == UserRole.ADMIN
                && current.isActive()
                && (newRole != UserRole.ADMIN || !active);
        if (removesActiveAdmin && userRepository.lockActiveByRole(UserRole.ADMIN).size() <= 1) {
            throw new ConflictException("O último administrador ativo não pode ser desativado ou rebaixado");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
