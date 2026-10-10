import { useState } from "react";
import { Link } from "react-router-dom";
import ReceptionPage from "../../components/ReceptionPage";
import usePolling from "../../hooks/usePolling";
import { recAppointments } from "../../api/staff";
import { getApiError } from "../../api/errors";

export default function ReceptionHome() {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState(null);

  usePolling(() => {
    recAppointments({}).then((r) => { setRows(r); setError(null); }).catch((e) => setError(getApiError(e).message));
  }, 30000, []);

  const count = (...s) => (rows ? rows.filter((r) => s.includes(r.status)).length : "-");
  const stats = [
    ["Expected", count("CONFIRMED", "ATTENDANCE_PENDING")],
    ["Waiting", count("CHECKED_IN", "WAITING")],
    ["In consultation", count("IN_CONSULTATION")],
    ["Completed", count("COMPLETED")],
    ["Cancelled / no-show", count("CANCELLED", "NO_SHOW")],
  ];
  const links = [
    ["/reception/check-in", "QR check-in", "Scan or enter a patient's code"],
    ["/reception/walk-in", "Register a walk-in", "Book and check in a patient at the desk"],
    ["/reception/queue", "Queue board", "Who is waiting for which doctor"],
    ["/reception/appointments", "All appointments", "Search, check in, cancel or mark no-show"],
  ];

  return (
    <ReceptionPage title="Reception">
      {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Today</h2>
      <div className="mt-3 grid gap-3 sm:grid-cols-3 lg:grid-cols-5">
        {stats.map(([label, n]) => (
          <div key={label} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-3xl font-bold text-slate-900">{n}</p>
            <p className="text-sm text-slate-500">{label}</p>
          </div>
        ))}
      </div>
      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        {links.map(([to, label, note]) => (
          <Link key={to} to={to} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm hover:border-brand-500">
            <h3 className="font-semibold text-slate-800">{label}</h3>
            <p className="mt-1 text-sm text-slate-500">{note}</p>
          </Link>
        ))}
      </div>
    </ReceptionPage>
  );
}