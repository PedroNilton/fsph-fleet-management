ALTER TABLE users
    DROP CONSTRAINT chk_users_role;

UPDATE users
SET role = 'GESTOR_FROTA'
WHERE role = 'COORDENADOR';

ALTER TABLE users
    RENAME COLUMN password TO password_hash;

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (
        role IN (
            'ADMIN',
            'EMPLOYEE',
            'GESTOR_FROTA',
            'MOTORISTA',
            'ATENDENTE_SVO',
            'GESTOR_SVO',
            'EQUIPE_COLETA_SVO'
        )
    );
