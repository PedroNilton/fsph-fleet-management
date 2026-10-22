ALTER TABLE role_permissions DROP CONSTRAINT chk_role_permissions_permission;
ALTER TABLE role_permissions ADD CONSTRAINT chk_role_permissions_permission CHECK (permission IN (
    'USERS_READ', 'USERS_WRITE', 'SECTORS_READ', 'SECTORS_WRITE',
    'PERMISSIONS_READ', 'PERMISSIONS_WRITE', 'VEHICLES_READ', 'VEHICLES_WRITE',
    'DRIVERS_READ', 'DRIVERS_WRITE'
));

INSERT INTO role_permissions (role, permission)
SELECT 'ADMIN', permission FROM (VALUES
    ('VEHICLES_READ'), ('VEHICLES_WRITE'), ('DRIVERS_READ'), ('DRIVERS_WRITE')
) AS permissions(permission);

INSERT INTO role_permissions (role, permission)
SELECT 'GESTOR_FROTA', permission FROM (VALUES
    ('VEHICLES_READ'), ('VEHICLES_WRITE'), ('DRIVERS_READ'), ('DRIVERS_WRITE')
) AS permissions(permission)
WHERE EXISTS (SELECT 1 FROM role_permissions
              WHERE role = 'GESTOR_FROTA' AND permission = 'SECTORS_READ')
  AND NOT EXISTS (SELECT 1 FROM role_permissions
                  WHERE role = 'GESTOR_FROTA' AND permission <> 'SECTORS_READ');

INSERT INTO role_permissions (role, permission)
SELECT 'MOTORISTA', 'VEHICLES_READ'
WHERE EXISTS (SELECT 1 FROM role_permissions
              WHERE role = 'MOTORISTA' AND permission = 'SECTORS_READ')
  AND NOT EXISTS (SELECT 1 FROM role_permissions
                  WHERE role = 'MOTORISTA' AND permission <> 'SECTORS_READ');
