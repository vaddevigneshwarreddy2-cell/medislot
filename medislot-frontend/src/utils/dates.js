export const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

// Local yyyy-MM-dd (not UTC), so "today" matches the user's calendar.
export function toDateString(d) {
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

export function todayString() {
  return toDateString(new Date());
}

export function parseDate(s) {
  return new Date(`${s}T00:00:00`);
}

export function formatLong(s) {
  return parseDate(s).toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long", year: "numeric" });
}

export function toMinutes(t) {
  const [h, m] = t.split(":").map(Number);
  return h * 60 + m;
}

export const REASONS = {
  DOCTOR_ON_LEAVE: "The doctor is on leave.",
  HOSPITAL_HOLIDAY: "The hospital is closed (holiday).",
  NOT_WORKING_DAY: "The doctor does not work on this day.",
  NO_SLOTS_LEFT: "No slots are left today.",
  PAST_DATE: "This date has passed.",
  TOO_FAR: "This date is too far ahead to book.",
};