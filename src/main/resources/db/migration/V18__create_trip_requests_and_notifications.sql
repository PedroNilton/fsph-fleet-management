CREATE TABLE trip_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_user_id UUID NOT NULL REFERENCES users (id),
    departure_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expected_return_at TIMESTAMP WITH TIME ZONE NOT NULL,
    origin VARCHAR(200) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    passenger_count INTEGER NOT NULL,
    requested_vehicle_id UUID REFERENCES vehicles (id),
    status VARCHAR(20) NOT NULL,
    assigned_vehicle_id UUID REFERENCES vehicles (id),
    assigned_driver_id UUID REFERENCES drivers (id),
    decided_by_user_id UUID REFERENCES users (id),
    decided_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(500),
    decision_notes VARCHAR(500),
    scheduled_by_user_id UUID REFERENCES users (id),
    scheduled_at TIMESTAMP WITH TIME ZONE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_trip_requests_period CHECK (expected_return_at > departure_at),
    CONSTRAINT chk_trip_requests_origin CHECK (btrim(origin) <> ''),
    CONSTRAINT chk_trip_requests_destination CHECK (btrim(destination) <> ''),
    CONSTRAINT chk_trip_requests_purpose CHECK (btrim(purpose) <> ''),
    CONSTRAINT chk_trip_requests_passengers CHECK (passenger_count >= 1),
    CONSTRAINT chk_trip_requests_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'SCHEDULED', 'CANCELED')
    ),
    CONSTRAINT chk_trip_requests_rejection_reason CHECK (
        (status = 'REJECTED' AND rejection_reason IS NOT NULL AND btrim(rejection_reason) <> '')
        OR (status <> 'REJECTED' AND rejection_reason IS NULL)
    ),
    CONSTRAINT chk_trip_requests_scheduled_allocation CHECK (
        status <> 'SCHEDULED'
        OR (
            assigned_vehicle_id IS NOT NULL
            AND assigned_driver_id IS NOT NULL
            AND scheduled_by_user_id IS NOT NULL
            AND scheduled_at IS NOT NULL
        )
    ),
    CONSTRAINT chk_trip_requests_pending_clean CHECK (
        status <> 'PENDING'
        OR (
            assigned_vehicle_id IS NULL
            AND assigned_driver_id IS NULL
            AND decided_by_user_id IS NULL
            AND decided_at IS NULL
            AND scheduled_by_user_id IS NULL
            AND scheduled_at IS NULL
            AND canceled_at IS NULL
        )
    )
);

CREATE INDEX ix_trip_requests_requester
    ON trip_requests (requester_user_id, created_at DESC);

CREATE INDEX ix_trip_requests_status_departure
    ON trip_requests (status, departure_at ASC);

CREATE INDEX ix_trip_requests_vehicle_schedule
    ON trip_requests (assigned_vehicle_id, departure_at, expected_return_at)
    WHERE status IN ('APPROVED', 'SCHEDULED') AND assigned_vehicle_id IS NOT NULL;

CREATE INDEX ix_trip_requests_driver_schedule
    ON trip_requests (assigned_driver_id, departure_at, expected_return_at)
    WHERE status = 'SCHEDULED' AND assigned_driver_id IS NOT NULL;

CREATE TABLE user_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL REFERENCES users (id),
    trip_request_id UUID REFERENCES trip_requests (id),
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_user_notifications_type CHECK (
        type IN ('TRIP_REQUEST_APPROVED', 'TRIP_REQUEST_REJECTED', 'TRIP_REQUEST_SCHEDULED')
    ),
    CONSTRAINT chk_user_notifications_title CHECK (btrim(title) <> ''),
    CONSTRAINT chk_user_notifications_message CHECK (btrim(message) <> ''),
    CONSTRAINT chk_user_notifications_read CHECK (
        (is_read = FALSE AND read_at IS NULL)
        OR (is_read = TRUE AND read_at IS NOT NULL)
    )
);

CREATE INDEX ix_user_notifications_recipient
    ON user_notifications (recipient_user_id, is_read, created_at DESC);
