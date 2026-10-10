CREATE TABLE notifications (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type           VARCHAR(40) NOT NULL,
    title          VARCHAR(150) NOT NULL,
    message        VARCHAR(500) NOT NULL,
    link           VARCHAR(200),
    appointment_id BIGINT REFERENCES appointments(id) ON DELETE SET NULL,
    dedupe_key     VARCHAR(100),
    read_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notif_user ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notif_unread ON notifications (user_id) WHERE read_at IS NULL;
-- The same event can never notify the same user twice.
CREATE UNIQUE INDEX uq_notif_dedupe ON notifications (user_id, dedupe_key) WHERE dedupe_key IS NOT NULL;

ALTER TABLE appointments
    ADD COLUMN attendance_requested_at TIMESTAMPTZ,
    ADD COLUMN attendance_confirmed_at TIMESTAMPTZ,
    ADD COLUMN reminder_sent_at TIMESTAMPTZ;
CREATE INDEX idx_appt_reminders ON appointments (slot_start)
    WHERE status IN ('CONFIRMED','ATTENDANCE_PENDING');