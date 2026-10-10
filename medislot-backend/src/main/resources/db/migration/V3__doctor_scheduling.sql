CREATE TABLE doctor_availability (
    id          BIGSERIAL PRIMARY KEY,
    doctor_id   BIGINT   NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL,
    start_time  TIME     NOT NULL,
    end_time    TIME     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_avail_day  CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_avail_time CHECK (end_time > start_time)
);
CREATE INDEX idx_avail_doctor_day ON doctor_availability (doctor_id, day_of_week);

CREATE TABLE doctor_leave (
    id         BIGSERIAL PRIMARY KEY,
    doctor_id  BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    leave_date DATE   NOT NULL,
    reason     VARCHAR(255),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_per_day UNIQUE (doctor_id, leave_date)
);

CREATE TABLE hospital_holidays (
    id           BIGSERIAL PRIMARY KEY,
    hospital_id  BIGINT NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    holiday_date DATE   NOT NULL,
    description  VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_holiday_per_day UNIQUE (hospital_id, holiday_date)
);