CREATE TABLE vehicle_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles (id),
    type VARCHAR(20) NOT NULL,
    document_number VARCHAR(100),
    issued_on DATE,
    expires_on DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_vehicle_documents_type CHECK (type IN ('LICENSING', 'INSURANCE', 'IPVA', 'INSPECTION')),
    CONSTRAINT chk_vehicle_documents_status CHECK (status IN ('ACTIVE', 'REPLACED')),
    CONSTRAINT chk_vehicle_documents_dates CHECK (issued_on IS NULL OR issued_on <= expires_on)
);

CREATE UNIQUE INDEX ux_vehicle_documents_active_type
    ON vehicle_documents (vehicle_id, type) WHERE status = 'ACTIVE';
CREATE INDEX ix_vehicle_documents_expiry ON vehicle_documents (expires_on) WHERE status = 'ACTIVE';
CREATE INDEX ix_vehicle_documents_history ON vehicle_documents (vehicle_id, type, created_at DESC);
