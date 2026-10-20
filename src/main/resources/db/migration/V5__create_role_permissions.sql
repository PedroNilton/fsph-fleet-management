CREATE TABLE role_permissions (
    role VARCHAR(30) NOT NULL,
    permission VARCHAR(40) NOT NULL,
    PRIMARY KEY (role, permission),
    CONSTRAINT chk_role_permissions_role CHECK (role IN (
        'ADMIN', 'EMPLOYEE', 'GESTOR_FROTA', 'MOTORISTA',
        'ATENDENTE_SVO', 'GESTOR_SVO', 'EQUIPE_COLETA_SVO'
    )),
    CONSTRAINT chk_role_permissions_permission CHECK (permission IN (
        'USERS_READ', 'USERS_WRITE', 'SECTORS_READ', 'SECTORS_WRITE',
        'PERMISSIONS_READ', 'PERMISSIONS_WRITE'
    ))
);

INSERT INTO role_permissions (role, permission)
SELECT 'ADMIN', permission FROM (VALUES
    ('USERS_READ'), ('USERS_WRITE'), ('SECTORS_READ'),
    ('SECTORS_WRITE'), ('PERMISSIONS_READ'), ('PERMISSIONS_WRITE')
) AS permissions(permission);

INSERT INTO role_permissions (role, permission)
SELECT role, 'SECTORS_READ' FROM (VALUES
    ('EMPLOYEE'), ('GESTOR_FROTA'), ('MOTORISTA'),
    ('ATENDENTE_SVO'), ('GESTOR_SVO'), ('EQUIPE_COLETA_SVO')
) AS roles(role);
