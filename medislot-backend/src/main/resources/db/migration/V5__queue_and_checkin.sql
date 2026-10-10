ALTER TABLE appointments
    ADD COLUMN queue_number INT,
    ADD COLUMN checked_in_at TIMESTAMPTZ,
    ADD COLUMN called_at TIMESTAMPTZ,
    ADD COLUMN consultation_started_at TIMESTAMPTZ,
    ADD COLUMN completed_at TIMESTAMPTZ;

-- One queue number per doctor per day, whatever the application does.
CREATE UNIQUE INDEX uq_appt_queue_number ON appointments (doctor_id, appointment_date, queue_number)
    WHERE queue_number IS NOT NULL;
CREATE INDEX idx_appt_hospital_date ON appointments (hospital_id, appointment_date);

CREATE TABLE check_ins (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id),
    hospital_id    BIGINT NOT NULL REFERENCES hospitals(id),
    checked_in_by  BIGINT REFERENCES users(id) ON DELETE SET NULL,
    method         VARCHAR(20) NOT NULL,
    checked_in_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_checkin_method CHECK (method IN ('QR','MANUAL','WALK_IN'))
);