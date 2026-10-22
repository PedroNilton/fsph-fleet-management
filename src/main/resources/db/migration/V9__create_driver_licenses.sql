CREATE TABLE driver_licenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id UUID NOT NULL REFERENCES drivers (id),
    license_number VARCHAR(30) NOT NULL,
    category VARCHAR(10) NOT NULL,
    issued_on DATE NOT NULL,
    expires_on DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_driver_licenses_number CHECK (btrim(license_number) <> ''),
    CONSTRAINT chk_driver_licenses_category CHECK (btrim(category) <> ''),
    CONSTRAINT chk_driver_licenses_dates CHECK (issued_on <= expires_on),
    CONSTRAINT chk_driver_licenses_status CHECK (status IN ('ACTIVE', 'REPLACED'))
);

CREATE UNIQUE INDEX ux_driver_licenses_active_driver
    ON driver_licenses (driver_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX ux_driver_licenses_active_number
    ON driver_licenses (upper(btrim(license_number))) WHERE status = 'ACTIVE';
CREATE INDEX ix_driver_licenses_expiry ON driver_licenses (expires_on) WHERE status = 'ACTIVE';
CREATE INDEX ix_driver_licenses_history ON driver_licenses (driver_id, created_at DESC);
