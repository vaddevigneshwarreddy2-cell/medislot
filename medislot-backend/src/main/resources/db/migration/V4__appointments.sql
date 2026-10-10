CREATE TABLE appointments (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL REFERENCES patients(id),
    doctor_id           BIGINT NOT NULL REFERENCES doctors(id),
    hospital_id         BIGINT NOT NULL REFERENCES hospitals(id),
    department_id       BIGINT NOT NULL REFERENCES departments(id),
    appointment_date    DATE NOT NULL,
    slot_start          TIMESTAMPTZ NOT NULL,
    slot_end            TIMESTAMPTZ NOT NULL,
    status              VARCHAR(30) NOT NULL,
    hold_expires_at     TIMESTAMPTZ,
    reason              VARCHAR(500),
    source              VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    rescheduled_from_id BIGINT REFERENCES appointments(id),
    reschedule_count    INT NOT NULL DEFAULT 0,
    cancelled_at        TIMESTAMPTZ,
    cancelled_by        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    cancel_reason       VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_appt_status CHECK (status IN ('HELD','CONFIRMED','ATTENDANCE_PENDING','CHECKED_IN',
        'WAITING','IN_CONSULTATION','COMPLETED','CANCELLED','NO_SHOW','EXPIRED','RESCHEDULED')),
    CONSTRAINT chk_appt_slot CHECK (slot_end > slot_start),
    CONSTRAINT chk_appt_hold CHECK (status <> 'HELD' OR hold_expires_at IS NOT NULL)
);

-- THE double-booking guard: only one row per doctor and start time may occupy a slot.
-- CANCELLED, NO_SHOW, EXPIRED and RESCHEDULED rows are outside the index, so they free the slot.
CREATE UNIQUE INDEX uq_appt_active_slot ON appointments (doctor_id, slot_start)
    WHERE status IN ('HELD','CONFIRMED','ATTENDANCE_PENDING','CHECKED_IN','WAITING','IN_CONSULTATION','COMPLETED');

CREATE INDEX idx_appt_patient ON appointments (patient_id, slot_start);
CREATE INDEX idx_appt_doctor_date ON appointments (doctor_id, appointment_date);
CREATE INDEX idx_appt_hold_expiry ON appointments (hold_expires_at) WHERE status = 'HELD';

CREATE TABLE appointment_status_history (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL REFERENCES appointments(id),
    from_status    VARCHAR(30),
    to_status      VARCHAR(30) NOT NULL,
    changed_by     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    note           VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_appt_history ON appointment_status_history (appointment_id, created_at);