const STYLES = {
  HELD: "bg-amber-100 text-amber-800",
  CONFIRMED: "bg-green-100 text-green-700",
  ATTENDANCE_PENDING: "bg-amber-100 text-amber-800",
  CHECKED_IN: "bg-sky-100 text-sky-700",
  WAITING: "bg-sky-100 text-sky-700",
  IN_CONSULTATION: "bg-indigo-100 text-indigo-700",
  COMPLETED: "bg-slate-200 text-slate-700",
  CANCELLED: "bg-red-100 text-red-700",
  NO_SHOW: "bg-red-100 text-red-700",
  EXPIRED: "bg-slate-200 text-slate-600",
  RESCHEDULED: "bg-slate-200 text-slate-600",
};

const LABELS = {
  HELD: "Held",
  CONFIRMED: "Confirmed",
  ATTENDANCE_PENDING: "Confirm attendance",
  CHECKED_IN: "Checked in",
  WAITING: "Waiting",
  IN_CONSULTATION: "In consultation",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
  NO_SHOW: "No-show",
  EXPIRED: "Expired",
  RESCHEDULED: "Rescheduled",
};

export default function StatusBadge({ status }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${STYLES[status] || "bg-slate-200 text-slate-700"}`}>
      {LABELS[status] || status}
    </span>
  );
}