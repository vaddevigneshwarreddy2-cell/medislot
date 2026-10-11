import { useEffect, useState } from "react";
import { Link, Navigate, useLocation, useParams } from "react-router-dom";
import { isCancel } from "axios";
import { cancelAppointment, getAppointment } from "../../api/appointments";
import { confirmAttendance } from "../../api/notifications";
import { getApiError } from "../../api/errors";
import StatusBadge from "../../components/StatusBadge";
import CancelDialog from "../../components/CancelDialog";
import QueuePanel from "../../components/QueuePanel";
import { directionsUrl, hasCoordinates, viewLocationUrl } from "../../utils/maps";
import { formatLong } from "../../utils/dates";
import { ACTIVE_STATUSES, to12h } from "../../utils/format";

export default function AppointmentDetails() {
  const { appointmentId } = useParams();
  const location = useLocation();
  const [appt, setAppt] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCancel, setShowCancel] = useState(false);
  const [busy, setBusy] = useState(false);
  const [cancelError, setCancelError] = useState(null);
  const [attendError, setAttendError] = useState(null);
  const [attendMsg, setAttendMsg] = useState(null);

  async function load(signal) {
    try {
      setAppt(await getAppointment(appointmentId, signal));
    } catch (err) {
      if (isCancel(err)) return;
      setError(getApiError(err).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const c = new AbortController();
    setLoading(true);
    load(c.signal);
    return () => c.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
  if (appt.status === "HELD") return <Navigate to={`/appointments/${appt.appointmentId}/confirm`} replace />;

  const h = appt.hospital;
  const active = ACTIVE_STATUSES.includes(appt.status);
  const changeable = appt.status === "CONFIRMED" || appt.status === "ATTENDANCE_PENDING";
  const inQueue = ["CHECKED_IN", "WAITING", "IN_CONSULTATION"].includes(appt.status);
  const banner = location.state?.justBooked
    ? "Appointment Confirmed"
    : location.state?.justRescheduled ? "Appointment Rescheduled" : null;

  async function doCancel(reason) {
    setBusy(true);
    setCancelError(null);
    try {
      const updated = await cancelAppointment(appt.appointmentId, reason);
      setAppt(updated);
      setShowCancel(false);
    } catch (err) {
      setCancelError(getApiError(err).message);
    } finally {
      setBusy(false);
    }
  }

  async function attend() {
    setBusy(true);
    setAttendError(null);
    try {
      await confirmAttendance(appt.appointmentId);
      setAttendMsg("Thanks, your attendance is confirmed.");
      await load();
    } catch (err) {
      setAttendError(getApiError(err).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="mx-auto max-w-2xl px-4 py-10">
      <Link to="/patient/appointments" className="text-sm font-medium text-brand-600 hover:underline">
        &larr; My appointments
      </Link>

      {banner && (
        <p role="status" className="mt-4 rounded-lg bg-green-50 px-4 py-3 text-lg font-semibold text-green-700">
          <span aria-hidden="true">&#10003;</span> {banner}
        </p>
      )}

      {appt.status === "ATTENDANCE_PENDING" && (
        <div role="alert" className="mt-4 rounded-lg border border-amber-300 bg-amber-50 p-4">
          <p className="font-semibold text-amber-900">Please confirm that you will attend.</p>
          <p className="mt-1 text-sm text-amber-800">
            Confirming helps the hospital plan the day. If you can't come, cancel so someone else can use the slot.
          </p>
          <div className="mt-3 flex flex-wrap gap-3">
            <button type="button" onClick={attend} disabled={busy}
                    className="rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-60">
              Yes, I will attend
            </button>
            {appt.canCancel && (
              <button type="button" onClick={() => { setCancelError(null); setShowCancel(true); }} disabled={busy}
                      className="rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-60">
                Cancel appointment
              </button>
            )}
          </div>
          {attendError && <p className="mt-2 text-sm text-red-700">{attendError}</p>}
        </div>
      )}
      {attendMsg && (
        <p role="status" className="mt-4 rounded-lg bg-green-50 px-4 py-3 text-sm text-green-700">{attendMsg}</p>
      )}

      <div className="mt-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">{appt.doctorName}</h1>
            <p className="text-slate-600">{appt.specialization} &middot; {appt.department}</p>
          </div>
          <StatusBadge status={appt.status} />
        </div>

        {h && (
          <p className="mt-4 text-slate-700">
            <span aria-hidden="true">&#127973;</span> {h.name}
            <span className="block text-sm text-slate-500"><span aria-hidden="true">&#128205;</span> {h.address}</span>
          </p>
        )}
        <p className="mt-3 font-medium text-slate-800">
          <span aria-hidden="true">&#128197;</span> {formatLong(appt.appointmentDate)}
        </p>
        <p className="font-medium text-slate-800">
          <span aria-hidden="true">&#128336;</span> {to12h(appt.appointmentTime)} - {to12h(appt.endTime)}
        </p>
        {appt.reason && <p className="mt-3 text-sm text-slate-600">Reason: {appt.reason}</p>}
        {appt.rescheduleCount > 0 && (
          <p className="mt-1 text-xs text-slate-500">Rescheduled {appt.rescheduleCount} time(s).</p>
        )}

        {inQueue && <QueuePanel appointmentId={appt.appointmentId} />}

        {appt.status === "COMPLETED" && (
          <div className="mt-6 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700">
            Your visit is complete.{" "}
            <Link to="/patient/prescriptions" className="font-medium text-brand-600 hover:underline">
              View your prescriptions
            </Link>{" "}
            or{" "}
            <Link to="/patient/follow-ups" className="font-medium text-brand-600 hover:underline">
              follow-ups
            </Link>.
          </div>
        )}

        <div className="mt-6 flex flex-wrap gap-3">
          {changeable && (
            <Link to={`/appointments/${appt.appointmentId}/qr`}
                  className="rounded-lg bg-slate-800 px-4 py-2 text-sm font-medium text-white hover:bg-slate-900">
              View QR Code
            </Link>
          )}
          {hasCoordinates(h) && (
            <a href={viewLocationUrl(h)} target="_blank" rel="noopener noreferrer"
               className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100">
              View Hospital Location
            </a>
          )}
          {active && hasCoordinates(h) && (
            <a href={directionsUrl(h)} target="_blank" rel="noopener noreferrer"
               className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
              <span aria-hidden="true">&#129517;</span> Get Directions in Google Maps
            </a>
          )}
        </div>

        {changeable && (
          <div className="mt-4 flex flex-wrap gap-3 border-t border-slate-100 pt-4">
            {appt.canReschedule ? (
              <Link to={`/appointments/${appt.appointmentId}/reschedule`}
                    className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium hover:bg-slate-100">
                Reschedule Appointment
              </Link>
            ) : (
              <button type="button" disabled
                      className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-400">
                Reschedule Appointment
              </button>
            )}
            <button type="button" onClick={() => { setCancelError(null); setShowCancel(true); }} disabled={!appt.canCancel}
                    className="rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50">
              Cancel Appointment
            </button>
            {(!appt.canCancel || !appt.canReschedule) && (
              <p className="w-full text-xs text-slate-500">
                Online changes are closed for this appointment (too close to the start time, or the reschedule limit was reached).
                Please contact the hospital.
              </p>
            )}
          </div>
        )}
      </div>

      {showCancel && (
        <CancelDialog busy={busy} error={cancelError} onClose={() => setShowCancel(false)} onConfirm={doCancel} />
      )}
    </section>
  );
}