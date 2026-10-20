package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.application.services.RolePermissionService;
import br.com.gestaofrota.api.v1.dtos.requests.ReplacePermissionsRequest;
import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.UserRole;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/roles")
public class RolePermissionController {
    private final RolePermissionService service;

    public RolePermissionController(RolePermissionService service) {
        this.service = service;
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSIONS_READ')")
    public Map<UserRole, Set<Permission>> list() {
        return service.list();
    }

    @GetMapping("/{role}/permissions")
    @PreAuthorize("hasAuthority('PERMISSIONS_READ')")
    public Set<Permission> find(@PathVariable UserRole role) {
        return service.find(role);
    }

    @PutMapping("/{role}/permissions")
    @PreAuthorize("hasAuthority('PERMISSIONS_WRITE')")
    public Set<Permission> replace(@PathVariable UserRole role, @Valid @RequestBody ReplacePermissionsRequest request) {
        return service.replace(role, request.permissions());
    }
}
