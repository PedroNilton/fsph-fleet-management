package br.com.gestaofrota.application.services;

import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.exceptions.ConflictException;
import br.com.gestaofrota.domain.models.RolePermission;
import br.com.gestaofrota.infra.repositories.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RolePermissionService {
    private final RolePermissionRepository repository;

    public RolePermissionService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Set<Permission> find(UserRole role) {
        if (role == UserRole.ADMIN) {
            return EnumSet.allOf(Permission.class);
        }

        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        repository.findAllById_Role(role).forEach(entry -> permissions.add(entry.getPermission()));

        return permissions;
    }

    @Transactional(readOnly = true)
    public Map<UserRole, Set<Permission>> list() {
        Map<UserRole, Set<Permission>> result = new EnumMap<>(UserRole.class);

        for (UserRole role : UserRole.values()) {
            result.put(role, find(role));
        }

        return result;
    }

    @Transactional
    public Set<Permission> replace(UserRole role, Set<Permission> permissions) {
        if (role == UserRole.ADMIN) {
            throw new ConflictException("As permissões de ADMIN são fixas");
        }

        EnumSet<Permission> requested = EnumSet.noneOf(Permission.class);
        requested.addAll(permissions);

        List<RolePermission> existing = repository.findAllById_Role(role);
        EnumSet<Permission> current = EnumSet.noneOf(Permission.class);

        List<RolePermission> removed = new ArrayList<>();
        for (RolePermission entry : existing) {
            current.add(entry.getPermission());
            if (!requested.contains(entry.getPermission())) {
                removed.add(entry);
            }
        }

        List<RolePermission> added = new ArrayList<>();
        for (Permission permission : requested) {
            if (!current.contains(permission)) {
                added.add(new RolePermission(role, permission));
            }
        }

        repository.deleteAll(removed);
        repository.saveAll(added);
        repository.flush();

        return find(role);
    }
}
