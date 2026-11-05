CREATE TABLE prime_fuel_stations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    address VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_prime_fuel_stations_name CHECK (btrim(name) <> ''),
    CONSTRAINT chk_prime_fuel_stations_cnpj CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT chk_prime_fuel_stations_city CHECK (btrim(city) <> ''),
    CONSTRAINT chk_prime_fuel_stations_state CHECK (state ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_prime_fuel_stations_address CHECK (btrim(address) <> '')
);

CREATE UNIQUE INDEX ux_prime_fuel_stations_cnpj
    ON prime_fuel_stations (cnpj);

CREATE INDEX ix_prime_fuel_stations_active_name
    ON prime_fuel_stations (is_active, name);

CREATE TABLE fuelings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles (id),
    station_id UUID NOT NULL REFERENCES prime_fuel_stations (id),
    registered_by_user_id UUID NOT NULL REFERENCES users (id),
    fueled_on DATE NOT NULL,
    odometer_km BIGINT NOT NULL,
    previous_odometer_km BIGINT,
    distance_driven_km BIGINT,
    liters NUMERIC(10, 3) NOT NULL,
    total_value NUMERIC(12, 2) NOT NULL,
    fuel_type VARCHAR(20) NOT NULL,
    consumption_km_per_liter NUMERIC(10, 2),
    reference_average_km_per_liter NUMERIC(10, 2),
    out_of_pattern BOOLEAN NOT NULL DEFAULT FALSE,
    anomaly_reason VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_fuelings_odometer CHECK (odometer_km >= 0),
    CONSTRAINT chk_fuelings_previous_odometer CHECK (
        previous_odometer_km IS NULL OR odometer_km >= previous_odometer_km
    ),
    CONSTRAINT chk_fuelings_distance CHECK (
        distance_driven_km IS NULL OR distance_driven_km >= 0
    ),
    CONSTRAINT chk_fuelings_liters CHECK (liters > 0),
    CONSTRAINT chk_fuelings_total_value CHECK (total_value > 0),
    CONSTRAINT chk_fuelings_fuel_type CHECK (
        fuel_type IN ('GASOLINE', 'ETHANOL', 'DIESEL', 'DIESEL_S10', 'CNG')
    ),
    CONSTRAINT chk_fuelings_consumption CHECK (
        consumption_km_per_liter IS NULL OR consumption_km_per_liter >= 0
    )
);

CREATE INDEX ix_fuelings_vehicle_history
    ON fuelings (vehicle_id, odometer_km DESC, fueled_on DESC, created_at DESC);

CREATE INDEX ix_fuelings_anomalies
    ON fuelings (out_of_pattern, fueled_on DESC, created_at DESC)
    WHERE out_of_pattern = TRUE;

CREATE INDEX ix_fuelings_user_history
    ON fuelings (registered_by_user_id, fueled_on DESC, created_at DESC);

