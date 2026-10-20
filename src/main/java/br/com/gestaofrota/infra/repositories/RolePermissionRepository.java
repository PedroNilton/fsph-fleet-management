package br.com.gestaofrota.infra.repositories;

import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.models.RolePermission;
import br.com.gestaofrota.domain.models.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findAllById_Role(UserRole role);
}
