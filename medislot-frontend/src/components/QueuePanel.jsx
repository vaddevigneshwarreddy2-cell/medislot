import { useState } from "react";
import usePolling from "../hooks/usePolling";
import { getQueueStatus } from "../api/staff";

export default function QueuePanel({ appointmentId }) {
  const [q, setQ] = useState(null);
  const [error, setError] = useState(false);

  usePolling(() => {
    getQueueStatus(appointmentId).then((r) => { setQ(r); setError(false); }).catch(() => setError(true));
  }, 15000, [appointmentId]);

  if (!q) return error ? <p className="mt-4 text-sm text-slate-500">Queue status is unavailable right now.</p> : null;

  if (q.status === "IN_CONSULTATION") {
    return (
      <div role="status" className="mt-6 rounded-lg bg-indigo-50 px-4 py-3 text-indigo-800">
        Your consultation is in progress.
      </div>
    );
  }
  if (q.status !== "WAITING" && q.status !== "CHECKED_IN") return null;

  return (
    <div className="mt-6 rounded-lg border border-sky-200 bg-sky-50 p-4" aria-live="polite">
      {q.called && (
        <p role="alert" className="mb-3 rounded-lg bg-green-100 px-3 py-2 font-semibold text-green-800">
          You are being called. Please go to the doctor's room.
        </p>
      )}
      <h2 className="text-sm font-semibold uppercase tracking-wide text-sky-800">Queue status</h2>
      <div className="mt-2 flex flex-wrap gap-6">
        <div><p className="text-xs text-slate-500">Your number</p><p className="text-3xl font-bold text-slate-900">#{q.queueNumber}</p></div>
        {q.patientsAhead != null && (
          <div><p className="text-xs text-slate-500">Patients ahead</p><p className="text-3xl font-bold text-slate-900">{q.patientsAhead}</p></div>
        )}
        {q.estimatedWaitMinutes != null && (
          <div><p className="text-xs text-slate-500">Rough wait</p><p className="text-3xl font-bold text-slate-900">~{q.estimatedWaitMinutes} min</p></div>
        )}
      </div>
      <p className="mt-2 text-xs text-slate-500">The wait is only an estimate and can change. This page refreshes automatically.</p>
    </div>
  );
}