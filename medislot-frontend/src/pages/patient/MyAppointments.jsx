import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { isCancel } from "axios";
import { cancelAppointment, myAppointments } from "../../api/appointments";
import { getApiError } from "../../api/errors";
import AppointmentCard from "../../components/AppointmentCard";
import CancelDialog from "../../components/CancelDialog";

export default function MyAppointments() {
  const [scope, setScope] = useState("upcoming");
  const [items, setItems] = useState([]);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [target, setTarget] = useState(null);
  const [busy, setBusy] = useState(false);
  const [cancelError, setCancelError] = useState(null);
  const [msg, setMsg] = useState(null);

  useEffect(() => {
    const c = new AbortController();
    setStatus("loading");
    myAppointments(scope, c.signal)
      .then((d) => { setItems(d); setStatus("done"); })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setStatus("error");
      });
    return () => c.abort();
  }, [scope, reloadKey]);

  async function doCancel(reason) {
    setBusy(true);
    setCancelError(null);
    try {
      await cancelAppointment(target.appointmentId, reason);
      setTarget(null);
      setMsg("Appointment cancelled.");
      setReloadKey((k) => k + 1);
    } catch (err) {
      setCancelError(getApiError(err).message);
    } finally {
      setBusy(false);
    }
  }

  const tab = (value, label) => (
    <button type="button" onClick={() => { setScope(value); setMsg(null); }} aria-pressed={scope === value}
            className={`rounded-full border px-4 py-1.5 text-sm font-medium ${
              scope === value ? "border-brand-600 bg-brand-600 text-white" : "border-slate-300 bg-white hover:bg-slate-100"}`}>
      {label}
    </button>
  );

  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-3xl font-bold text-slate-900">My appointments</h1>
      <div className="mt-4 flex gap-2" role="group" aria-label="Appointment list">
        {tab("upcoming", "Upcoming")}
        {tab("history", "History")}
      </div>

      {msg && <p role="status" className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}

      <div className="mt-6" aria-live="polite">
        {status === "loading" && <p className="p-8 text-center text-slate-500">Loading...</p>}
        {status === "error" && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-red-700">{error}</p>}

        {status === "done" && items.length === 0 && (
          <div className="rounded-xl border border-dashed border-slate-300 p-10 text-center">
            <p className="text-slate-600">
              {scope === "upcoming" ? "You have no upcoming appointments." : "No past appointments yet."}
            </p>
            {scope === "upcoming" && (
              <Link to="/doctors" className="mt-4 inline-block rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700">
                Find a doctor
              </Link>
            )}
          </div>
        )}

        {status === "done" && items.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2">
            {items.map((a) => (
              <AppointmentCard key={a.appointmentId} appt={a}
                               onCancel={scope === "upcoming" ? (x) => { setCancelError(null); setTarget(x); } : undefined} />
            ))}
          </div>
        )}
      </div>

      {target && <CancelDialog busy={busy} error={cancelError} onClose={() => setTarget(null)} onConfirm={doCancel} />}
    </section>
  );
}