package br.com.gestaofrota.infra.security.models;

import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.enums.Permission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        UUID sectorId,
        String name,
        String email,
        String password,
        UserRole role,
        boolean active,
        Set<Permission> permissions
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        permissions.stream().map(
                permission -> new SimpleGrantedAuthority(permission.name())).forEach(authorities::add
        );

        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
