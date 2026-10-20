package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.UserRole;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    protected RolePermission() {
    }

    public RolePermission(UserRole role, Permission permission) {
        this.id = new RolePermissionId(role, permission);
    }

    public RolePermissionId getId() {
        return id;
    }

    public Permission getPermission() {
        return id.getPermission();
    }
}
