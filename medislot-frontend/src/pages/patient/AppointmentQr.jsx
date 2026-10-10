import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getQr, getQueueStatus } from "../../api/staff";
import { getApiError } from "../../api/errors";
import usePolling from "../../hooks/usePolling";
import { formatLong } from "../../utils/dates";
import { to12h } from "../../utils/format";

export default function AppointmentQr() {
  const { appointmentId } = useParams();
  const navigate = useNavigate();
  const [qr, setQr] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    getQr(appointmentId)
      .then((r) => { setQr(r); setLoading(false); })
      .catch((err) => { setError(getApiError(err).message); setLoading(false); });
  }, [appointmentId]);

  // Once reception has checked you in, the QR is no longer needed.
  usePolling(() => {
    if (!qr) return;
    getQueueStatus(appointmentId)
      .then((s) => {
        if (s.status !== "CONFIRMED" && s.status !== "ATTENDANCE_PENDING") {
          navigate(`/appointments/${appointmentId}`, { replace: true });
        }
      })
      .catch(() => {});
  }, 10000, [qr]);

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;

  if (error || !qr) {
    return (
      <section className="mx-auto max-w-md px-4 py-16 text-center">
        <p role="alert" className="text-lg text-slate-700">{error || "QR code not available."}</p>
        <Link to={`/appointments/${appointmentId}`} className="mt-4 inline-block font-medium text-brand-600 hover:underline">
          Back to the appointment
        </Link>
      </section>
    );
  }

  return (
    <section className="mx-auto max-w-md px-4 py-10 text-center">
      <Link to={`/appointments/${appointmentId}`} className="text-sm font-medium text-brand-600 hover:underline">
        &larr; Back to the appointment
      </Link>
      <h1 className="mt-3 text-2xl font-bold text-slate-900">Your check-in code</h1>
      <p className="mt-1 text-slate-600">
        {formatLong(qr.appointmentDate)} at {to12h(qr.appointmentTime)}
      </p>

      <div className="mt-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <img src={qr.imageDataUrl} alt="Check-in QR code for your appointment"
             className="mx-auto h-64 w-64" />
        <p className="mt-4 break-all font-mono text-sm text-slate-700">{qr.code}</p>
      </div>

      <p className="mt-4 text-sm text-slate-600">
        Show this code at the reception desk when you arrive. Check-in opens shortly before your appointment.
      </p>
      <p className="mt-1 text-xs text-slate-500">
        The code contains no personal or medical information, but please don't share it. If you reschedule, you get a new code.
      </p>
    </section>
  );
}