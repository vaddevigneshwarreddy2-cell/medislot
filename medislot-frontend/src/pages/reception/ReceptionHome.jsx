import { useState } from "react";
import { Link } from "react-router-dom";
import ReceptionPage from "../../components/ReceptionPage";
import usePolling from "../../hooks/usePolling";
import api from "../../api/axios";
import { changeStatus, checkInById, recAppointments } from "../../api/staff";
import { getApiError } from "../../api/errors";
import { to12h } from "../../utils/format";

export default function ReceptionHome() {
  const [rows, setRows] = useState(null);
  const [attention, setAttention] = useState([]);
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);
  const [reload, setReload] = useState(0);

  usePolling(() => {
    Promise.all([recAppointments({}), api.get("/reception/attention").then((r) => r.data.data)])
      .then(([r, a]) => { setRows(r); setAttention(a); setError(null); })
      .catch((e) => setError(getApiError(e).message));
  }, 30000, [reload]);

  async function act(fn, success) {
    setError(null);
    setMsg(null);
    try { await fn(); setMsg(success); setReload((n) => n + 1); }
    catch (e) { setError(getApiError(e).message); }
  }

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
      <div aria-live="polite">
        {msg && <p role="status" className="mb-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}
        {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      </div>
      <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Today</h2>
      <div className="mt-3 grid gap-3 sm:grid-cols-3 lg:grid-cols-5">
        {stats.map(([label, n]) => (
          <div key={label} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-3xl font-bold text-slate-900">{n}</p>
            <p className="text-sm text-slate-500">{label}</p>
          </div>
        ))}
      </div>

      <h2 className="mt-8 text-sm font-semibold uppercase tracking-wide text-slate-500">
        Needs attention ({attention.length})
      </h2>
      <p className="text-xs text-slate-500">Booked, not checked in, and the appointment time has passed. Nothing is marked automatically.</p>
      {attention.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">Nothing needs attention.</p>
      ) : (
        <ul className="mt-2 divide-y divide-slate-100 rounded-xl border border-amber-200 bg-amber-50">
          {attention.map((a) => (
            <li key={a.appointmentId} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
              <div>
                <p className="font-medium text-slate-900">{a.patientName} <span className="text-sm text-slate-500">{a.patientMobile}</span></p>
                <p className="text-xs text-slate-600">{a.appointmentDate} {to12h(a.appointmentTime)} &middot; {a.doctorName}</p>
              </div>
              <div className="flex gap-3 text-sm font-medium">
                {String(a.appointmentDate) === new Date().toLocaleDateString("en-CA") && (
                  <button type="button" onClick={() => act(() => checkInById(a.appointmentId), `${a.patientName} checked in.`)}
                          className="text-brand-600 hover:underline">Check in</button>
                )}
                <button type="button"
                        onClick={() => { if (window.confirm(`Mark ${a.patientName} as a no-show? This cannot be undone.`))
                          act(() => changeStatus(a.appointmentId, "NO_SHOW", "Did not arrive"), "Marked as no-show."); }}
                        className="text-red-600 hover:underline">No-show</button>
              </div>
            </li>
          ))}
        </ul>
      )}

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