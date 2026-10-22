CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sector_id UUID REFERENCES sectors (id),
    plate VARCHAR(10) NOT NULL,
    model VARCHAR(150) NOT NULL,
    capacity INTEGER NOT NULL,
    usage_type VARCHAR(50) NOT NULL,
    current_odometer_km BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_vehicles_plate CHECK (plate ~ '^[A-Za-z0-9]{7,10}$'),
    CONSTRAINT chk_vehicles_model CHECK (btrim(model) <> ''),
    CONSTRAINT chk_vehicles_usage_type CHECK (btrim(usage_type) <> ''),
    CONSTRAINT chk_vehicles_capacity CHECK (capacity > 0),
    CONSTRAINT chk_vehicles_odometer CHECK (current_odometer_km >= 0)
);

CREATE UNIQUE INDEX ux_vehicles_plate_normalized ON vehicles (upper(btrim(plate)));
CREATE INDEX ix_vehicles_sector_id ON vehicles (sector_id);
