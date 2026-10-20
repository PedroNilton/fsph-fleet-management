package br.com.gestaofrota.domain.models;

import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RolePermissionId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 40)
    private Permission permission;

    protected RolePermissionId() {
    }

    public RolePermissionId(UserRole role, Permission permission) {
        this.role = Objects.requireNonNull(role);
        this.permission = Objects.requireNonNull(permission);
    }

    public UserRole getRole() {
        return role;
    }

    public Permission getPermission() {
        return permission;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RolePermissionId that)) {
            return false;
        }
        return role == that.role && permission == that.permission;
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, permission);
    }
}
