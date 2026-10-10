CREATE TABLE consultations (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id),
    doctor_id      BIGINT NOT NULL REFERENCES doctors(id),
    patient_id     BIGINT NOT NULL REFERENCES patients(id),
    hospital_id    BIGINT NOT NULL REFERENCES hospitals(id),
    notes          TEXT,
    diagnosis      TEXT,
    instructions   TEXT,
    follow_up_date DATE,
    finalized      BOOLEAN NOT NULL DEFAULT FALSE,
    finalized_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_consult_patient ON consultations (patient_id, finalized);
CREATE INDEX idx_consult_doctor ON consultations (doctor_id, finalized);

CREATE TABLE prescriptions (
    id              BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL UNIQUE REFERENCES consultations(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE prescription_items (
    id              BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    sort_order      INT NOT NULL,
    medicine_name   VARCHAR(150) NOT NULL,
    dosage          VARCHAR(100) NOT NULL,
    frequency       VARCHAR(100) NOT NULL,
    duration        VARCHAR(100) NOT NULL,
    instructions    VARCHAR(255)
);
CREATE INDEX idx_rx_items ON prescription_items (prescription_id, sort_order);