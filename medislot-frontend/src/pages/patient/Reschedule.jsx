import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { isCancel } from "axios";
import { getAppointment, rescheduleAppointment } from "../../api/appointments";
import { getApiError } from "../../api/errors";
import SlotPicker from "../../components/SlotPicker";
import { formatLong } from "../../utils/dates";
import { to12h } from "../../utils/format";

export default function Reschedule() {
  const { appointmentId } = useParams();
  const navigate = useNavigate();
  const [appt, setAppt] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    const c = new AbortController();
    setLoading(true);
    getAppointment(appointmentId, c.signal)
      .then((a) => { setAppt(a); setLoading(false); })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setLoading(false);
      });
    return () => c.abort();
  }, [appointmentId]);

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;
  if (error || !appt) {
    return (
      <section className="mx-auto max-w-xl px-4 py-16 text-center">
        <p role="alert" className="text-lg text-slate-700">{error || "Appointment not found."}</p>
        <Link to="/patient/appointments" className="mt-4 inline-block font-medium text-brand-600 hover:underline">My appointments</Link>
      </section>
    );
  }
  if (!appt.canReschedule || !appt.doctorId) {
    return (
      <section className="mx-auto max-w-xl px-4 py-16 text-center">
        <p className="text-lg text-slate-700">This appointment can no longer be rescheduled online.</p>
        <Link to={`/appointments/${appt.appointmentId}`} className="mt-4 inline-block font-medium text-brand-600 hover:underline">
          Back to the appointment
        </Link>
      </section>
    );
  }

  async function submit() {
    if (!selected) return;
    setBusy(true);
    setActionError(null);
    try {
      const n = await rescheduleAppointment(appt.appointmentId, selected.date, selected.time);
      navigate(`/appointments/${n.appointmentId}`, { replace: true, state: { justRescheduled: true } });
    } catch (err) {
      const { code, message } = getApiError(err);
      setActionError(message);
      if (code === "SLOT_UNAVAILABLE") {
        setSelected(null);
        setRefreshKey((k) => k + 1);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="mx-auto max-w-2xl px-4 py-10">
      <Link to={`/appointments/${appt.appointmentId}`} className="text-sm font-medium text-brand-600 hover:underline">
        &larr; Back to the appointment
      </Link>
      <h1 className="mt-2 text-2xl font-bold text-slate-900">Reschedule appointment</h1>
      <p className="mt-1 text-slate-600">
        {appt.doctorName} &middot; currently {formatLong(appt.appointmentDate)} at {to12h(appt.appointmentTime)}
      </p>

      <div className="mt-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Choose a new date and time</h2>
        <SlotPicker doctorId={appt.doctorId} selected={selected} onSelect={setSelected} refreshKey={refreshKey} />

        {actionError && (
          <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{actionError}</p>
        )}
        {selected && (
          <p className="mt-4 text-sm text-slate-700">
            New time: <span className="font-semibold">{formatLong(selected.date)} at {to12h(selected.time)}</span>
          </p>
        )}
        <button type="button" onClick={submit} disabled={!selected || busy}
                className="mt-4 rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-50">
          {busy ? "Rescheduling..." : "Confirm new time"}
        </button>
      </div>
    </section>
  );
}