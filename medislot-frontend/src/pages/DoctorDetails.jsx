import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { isCancel } from "axios";
import { getDoctor } from "../api/doctors";
import { holdSlot } from "../api/appointments";
import { getApiError } from "../api/errors";
import { useAuth } from "../auth/AuthContext";
import SlotPicker from "../components/SlotPicker";
import { DAYS, formatLong } from "../utils/dates";
import { to12h } from "../utils/format";

export default function DoctorDetails() {
  const { doctorId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [doctor, setDoctor] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [booking, setBooking] = useState(false);
  const [bookError, setBookError] = useState(null);

  useEffect(() => {
    const c = new AbortController();
    setLoading(true);
    getDoctor(doctorId, c.signal)
      .then((d) => { setDoctor(d); setLoading(false); })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setLoading(false);
      });
    return () => c.abort();
  }, [doctorId]);

  async function book() {
    if (!selected) return;
    if (!user) {
      navigate("/login", { state: { from: location, message: "Log in to book an appointment." } });
      return;
    }
    setBooking(true);
    setBookError(null);
    try {
      const a = await holdSlot(doctorId, selected.date, selected.time);
      navigate(`/appointments/${a.appointmentId}/confirm`);
    } catch (err) {
      const { code, message } = getApiError(err);
      setBookError(message);
      if (code === "SLOT_UNAVAILABLE") {
        setSelected(null);
        setRefreshKey((k) => k + 1); // reload the slots so the taken one disappears
      }
    } finally {
      setBooking(false);
    }
  }

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;
  if (error || !doctor) {
    return (
      <section className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p role="alert" className="text-lg text-slate-700">{error || "Doctor not found."}</p>
        <Link to="/doctors" className="mt-4 inline-block font-medium text-brand-600 hover:underline">Back to doctor search</Link>
      </section>
    );
  }

  const d = doctor;
  const canBook = !user || user.role === "PATIENT";
  const byDay = {};
  d.schedule.forEach((w) => { (byDay[w.dayOfWeek] ||= []).push(`${w.start}-${w.end}`); });

  return (
    <section className="mx-auto max-w-3xl px-4 py-10">
      <Link to="/doctors" className="text-sm font-medium text-brand-600 hover:underline">&larr; Back to doctors</Link>

      <div className="mt-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="text-3xl font-bold text-slate-900">{d.fullName}</h1>
        <p className="font-medium text-brand-700">{d.specialization}</p>
        {d.qualification && <p className="mt-1 text-slate-600">{d.qualification}</p>}
        {d.experienceYears != null && <p className="text-slate-600">{d.experienceYears} years of experience</p>}
        <p className="mt-3 text-slate-700">
          <span aria-hidden="true">&#127973;</span>{" "}
          <Link to={`/hospitals/${d.hospitalId}`} className="text-brand-600 hover:underline">{d.hospitalName}</Link>, {d.hospitalCity}
        </p>
        <p className="text-sm text-slate-600">Department: {d.departmentName} | Appointment length: {d.consultationDurationMin} min</p>
        {d.bio && <p className="mt-3 text-sm text-slate-600">{d.bio}</p>}

        <h2 className="mt-8 text-sm font-semibold uppercase tracking-wide text-slate-500">Choose a date and time</h2>
        <div className="mt-3">
          <SlotPicker doctorId={doctorId} selected={selected} onSelect={setSelected} refreshKey={refreshKey} />
        </div>

        {bookError && (
          <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{bookError}</p>
        )}

        {selected && (
          <p className="mt-4 text-sm text-slate-700">
            Selected: <span className="font-semibold">{formatLong(selected.date)} at {to12h(selected.time)}</span>
          </p>
        )}

        {!canBook && (
          <p className="mt-4 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">
            Only patient accounts can book appointments.
          </p>
        )}

        <button type="button" onClick={book} disabled={!selected || booking || !canBook}
                className="mt-4 rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-50">
          {booking ? "Holding slot..." : user ? "Hold this slot and continue" : "Log in to book"}
        </button>

        <h2 className="mt-8 text-sm font-semibold uppercase tracking-wide text-slate-500">Weekly schedule</h2>
        {d.schedule.length === 0 ? (
          <p className="mt-2 text-sm text-slate-500">No working hours set yet.</p>
        ) : (
          <ul className="mt-2 space-y-1 text-sm text-slate-700">
            {DAYS.map((name, i) => byDay[i + 1] && (
              <li key={name}><span className="inline-block w-28 font-medium">{name}</span>{byDay[i + 1].join(", ")}</li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}