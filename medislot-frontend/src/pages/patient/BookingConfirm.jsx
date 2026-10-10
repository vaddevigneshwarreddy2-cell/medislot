import { useEffect, useState } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import { isCancel } from "axios";
import { cancelAppointment, confirmAppointment, getAppointment } from "../../api/appointments";
import { getApiError } from "../../api/errors";
import { formatLong } from "../../utils/dates";
import { mmss, to12h } from "../../utils/format";

export default function BookingConfirm() {
  const { appointmentId } = useParams();
  const navigate = useNavigate();
  const [appt, setAppt] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [forcedExpired, setForcedExpired] = useState(false);
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);

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
        <Link to="/patient/appointments" className="mt-4 inline-block font-medium text-brand-600 hover:underline">
          My appointments
        </Link>
      </section>
    );
  }
  if (appt.status === "CONFIRMED") return <Navigate to={`/appointments/${appt.appointmentId}`} replace />;

  const held = appt.status === "HELD";
  const remaining = held && appt.holdExpiresAt ? Math.max(0, Date.parse(appt.holdExpiresAt) - now) : 0;
  const expired = forcedExpired || appt.status === "EXPIRED" || (held && remaining === 0);
  const unusable = !held && appt.status !== "EXPIRED";
  const doctorLink = appt.doctorId ? `/doctors/${appt.doctorId}` : "/doctors";

  async function confirm() {
    setBusy(true);
    setActionError(null);
    try {
      await confirmAppointment(appt.appointmentId, reason.trim());
      navigate(`/appointments/${appt.appointmentId}`, { replace: true, state: { justBooked: true } });
    } catch (err) {
      const { code, message } = getApiError(err);
      if (code === "HOLD_EXPIRED") setForcedExpired(true);
      else setActionError(message);
    } finally {
      setBusy(false);
    }
  }

  async function release() {
    setBusy(true);
    try {
      await cancelAppointment(appt.appointmentId);
    } catch {
      // already released or expired: nothing more to do
    }
    navigate(doctorLink);
  }

  return (
    <section className="mx-auto max-w-xl px-4 py-10">
      <h1 className="text-2xl font-bold text-slate-900">Confirm your appointment</h1>

      <div className="mt-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="text-lg font-semibold text-slate-900">{appt.doctorName}</p>
        <p className="text-sm text-slate-600">{appt.specialization} &middot; {appt.department}</p>
        {appt.hospital && (
          <p className="mt-3 text-sm text-slate-700">
            <span aria-hidden="true">&#127973;</span> {appt.hospital.name}
            <span className="block text-slate-500">{appt.hospital.address}</span>
          </p>
        )}
        <p className="mt-3 font-medium text-slate-800">
          <span aria-hidden="true">&#128197;</span> {formatLong(appt.appointmentDate)}{" "}
          <span aria-hidden="true">&#128336;</span> {to12h(appt.appointmentTime)}
        </p>

        {expired ? (
          <div className="mt-6 rounded-lg bg-slate-50 p-4 text-center">
            <p className="font-semibold text-slate-800">Your hold on this slot has expired.</p>
            <p className="mt-1 text-sm text-slate-600">Slots are held for a few minutes only. Please choose a slot again.</p>
            <Link to={doctorLink} className="mt-4 inline-block rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700">
              Choose a slot again
            </Link>
          </div>
        ) : unusable ? (
          <div className="mt-6 rounded-lg bg-slate-50 p-4 text-center">
            <p className="font-semibold text-slate-800">This booking can no longer be confirmed.</p>
            <Link to="/patient/appointments" className="mt-3 inline-block font-medium text-brand-600 hover:underline">
              My appointments
            </Link>
          </div>
        ) : (
          <>
            <p
              role="timer"
              aria-live="off"
              className={`mt-6 rounded-lg px-3 py-2 text-sm ${remaining < 60000 ? "bg-red-50 text-red-700" : "bg-amber-50 text-amber-800"}`}
            >
              We are holding this slot for you. Time left: <span className="font-semibold">{mmss(remaining)}</span>
            </p>

            <label htmlFor="reason" className="mt-5 block text-sm font-medium text-slate-700">
              Reason for visit (optional)
            </label>
            <textarea
              id="reason"
              rows={3}
              maxLength={500}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <p className="mt-1 text-xs text-slate-500">Keep it short. Only your doctor and the hospital can see this.</p>

            {actionError && (
              <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{actionError}</p>
            )}

            <div className="mt-6 flex flex-wrap gap-3">
              <button type="button" onClick={confirm} disabled={busy}
                      className="rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
                {busy ? "Please wait..." : "Confirm appointment"}
              </button>
              <button type="button" onClick={release} disabled={busy}
                      className="rounded-lg border border-slate-300 px-5 py-2 font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60">
                Release slot
              </button>
            </div>
          </>
        )}
      </div>
    </section>
  );
}