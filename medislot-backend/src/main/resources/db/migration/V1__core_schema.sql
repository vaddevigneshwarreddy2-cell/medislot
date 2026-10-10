CREATE TABLE users (
    id                    BIGSERIAL PRIMARY KEY,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    mobile                VARCHAR(20)  UNIQUE,
    password_hash         VARCHAR(100) NOT NULL,
    full_name             VARCHAR(150) NOT NULL,
    role                  VARCHAR(30)  NOT NULL,
    verified              BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN
        ('PATIENT','DOCTOR','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')),
    CONSTRAINT chk_users_email_lower CHECK (email = lower(email))
);

CREATE TABLE patients (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    date_of_birth DATE,
    gender        VARCHAR(20),
    no_show_count INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE hospitals (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    state        VARCHAR(100) NOT NULL,
    postal_code  VARCHAR(20),
    country      VARCHAR(100) NOT NULL DEFAULT 'India',
    latitude     DECIMAL(10,7) NOT NULL,
    longitude    DECIMAL(10,7) NOT NULL,
    phone        VARCHAR(20),
    email        VARCHAR(255),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_hospitals_lat CHECK (latitude  BETWEEN -90  AND 90),
    CONSTRAINT chk_hospitals_lng CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX idx_hospitals_lat_lng ON hospitals (latitude, longitude);
CREATE INDEX idx_hospitals_name_lower ON hospitals (lower(name));

CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_department_per_hospital UNIQUE (hospital_id, name)
);

CREATE TABLE doctors (
    id                        BIGSERIAL PRIMARY KEY,
    user_id                   BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    hospital_id               BIGINT NOT NULL REFERENCES hospitals(id),
    department_id             BIGINT NOT NULL REFERENCES departments(id),
    specialization            VARCHAR(100) NOT NULL,
    qualification             VARCHAR(200),
    experience_years          INT,
    consultation_duration_min INT NOT NULL DEFAULT 30,
    bio                       VARCHAR(1000),
    active                    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_doctor_duration CHECK (consultation_duration_min BETWEEN 5 AND 240)
);
CREATE INDEX idx_doctors_hospital ON doctors (hospital_id);
CREATE INDEX idx_doctors_department ON doctors (department_id);

CREATE TABLE hospital_staff (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    hospital_id BIGINT NOT NULL REFERENCES hospitals(id),
    staff_role  VARCHAR(30) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_role CHECK (staff_role IN ('RECEPTIONIST','HOSPITAL_ADMIN'))
);
CREATE INDEX idx_staff_hospital ON hospital_staff (hospital_id);

CREATE TABLE verification_codes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash  VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts   INT NOT NULL DEFAULT 0,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_verification_user ON verification_codes (user_id, created_at DESC);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);

CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    actor_email   VARCHAR(255),
    action        VARCHAR(60) NOT NULL,
    resource_type VARCHAR(60),
    resource_id   VARCHAR(60),
    metadata      TEXT,
    ip_address    VARCHAR(45),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
CREATE INDEX idx_audit_actor ON audit_logs (actor_user_id);