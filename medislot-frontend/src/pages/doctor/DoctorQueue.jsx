import { useState } from "react";
import { Link } from "react-router-dom";
import StatusBadge from "../../components/StatusBadge";
import usePolling from "../../hooks/usePolling";
import { callNext, callPatient, completeConsultation, doctorQueue, startConsultation } from "../../api/staff";
import { getApiError } from "../../api/errors";
import { to12h } from "../../utils/format";

export default function DoctorQueue() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);
  const [busy, setBusy] = useState(false);
  const [reload, setReload] = useState(0);

  usePolling(() => {
    doctorQueue().then((r) => { setData(r); setError(null); }).catch((e) => setError(getApiError(e).message));
  }, 10000, [reload]);

  async function act(fn, success) {
    setBusy(true);
    setError(null);
    setMsg(null);
    try {
      await fn();
      setMsg(success);
      setReload((n) => n + 1);
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setBusy(false);
    }
  }

  const entries = data?.entries || [];
  const now = entries.filter((e) => e.status === "IN_CONSULTATION");
  const waiting = entries.filter((e) => e.status === "WAITING" || e.status === "CHECKED_IN");
  const expected = entries.filter((e) => e.status === "CONFIRMED" || e.status === "ATTENDANCE_PENDING");
  const btn = "rounded-lg px-3 py-1.5 text-sm font-medium disabled:opacity-60";

  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <Link to="/doctor" className="text-sm font-medium text-brand-600 hover:underline">&larr; Dashboard</Link>
      <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-3xl font-bold text-slate-900">Today's queue</h1>
        <button type="button" disabled={busy || waiting.length === 0} onClick={() => act(callNext, "Next patient called.")}
                className={`${btn} bg-brand-600 text-white hover:bg-brand-700 !px-5 !py-2`}>
          Call next patient
        </button>
      </div>
      <p className="mt-1 text-sm text-slate-500">
        {data ? `${data.completedToday} completed today.` : ""} Refreshes every 10 seconds.
      </p>

      <div aria-live="polite">
        {msg && <p role="status" className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}
        {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      </div>

      <h2 className="mt-8 text-sm font-semibold uppercase tracking-wide text-slate-500">Now seeing</h2>
      {now.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">No consultation in progress.</p>
      ) : now.map((e) => (
        <div key={e.appointmentId} className="mt-2 rounded-xl border border-indigo-200 bg-indigo-50 p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-lg font-semibold text-slate-900">{e.patientName}</p>
              <p className="text-sm text-slate-600">Slot {to12h(e.appointmentTime)}</p>
              {e.reason && <p className="mt-1 text-sm text-slate-700">Reason: {e.reason}</p>}
            </div>
            <button type="button" disabled={busy}
                    onClick={() => { if (window.confirm("Complete this consultation?")) act(() => completeConsultation(e.appointmentId), "Consultation completed."); }}
                    className={`${btn} bg-indigo-600 text-white hover:bg-indigo-700`}>
              Complete consultation
            </button>
          </div>
          <p className="mt-2 text-xs text-slate-500">Notes, prescription and follow-up arrive in the next phase.</p>
        </div>
      ))}

      <h2 className="mt-8 text-sm font-semibold uppercase tracking-wide text-slate-500">Waiting ({waiting.length})</h2>
      {waiting.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">Nobody is waiting.</p>
      ) : (
        <ul className="mt-2 divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white shadow-sm">
          {waiting.map((e) => (
            <li key={e.appointmentId} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
              <div>
                <p className="font-medium text-slate-800">
                  <span className="mr-2 text-slate-400">{e.position}.</span>{e.patientName}
                  {e.called && <span className="ml-2 rounded-full bg-green-100 px-2 py-0.5 text-xs font-semibold text-green-700">Called</span>}
                </p>
                <p className="text-xs text-slate-500">
                  Slot {to12h(e.appointmentTime)} &middot; queue #{e.queueNumber}{e.source === "WALK_IN" ? " - walk-in" : ""}
                </p>
                {e.reason && <p className="text-xs text-slate-600">Reason: {e.reason}</p>}
              </div>
              <div className="flex gap-2">
                {!e.called && (
                  <button type="button" disabled={busy} onClick={() => act(() => callPatient(e.appointmentId), "Patient called.")}
                          className={`${btn} border border-slate-300 hover:bg-slate-100`}>Call</button>)}
                <button type="button" disabled={busy || now.length > 0} title={now.length > 0 ? "Finish the current consultation first" : ""}
                        onClick={() => act(() => startConsultation(e.appointmentId), "Consultation started.")}
                        className={`${btn} bg-brand-600 text-white hover:bg-brand-700`}>Start</button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <h2 className="mt-8 text-sm font-semibold uppercase tracking-wide text-slate-500">Expected, not arrived ({expected.length})</h2>
      {expected.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">No upcoming bookings for today.</p>
      ) : (
        <ul className="mt-2 divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white shadow-sm">
          {expected.map((e) => (
            <li key={e.appointmentId} className="flex items-center justify-between px-4 py-3 text-sm">
              <span><span className="font-medium text-slate-800">{e.patientName}</span>
                <span className="ml-2 text-slate-500">{to12h(e.appointmentTime)}</span></span>
              <StatusBadge status={e.status} />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}