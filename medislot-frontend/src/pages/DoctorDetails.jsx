import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { isCancel } from "axios";
import { getAvailability, getDoctor, getUpcoming } from "../api/doctors";
import { getApiError } from "../api/errors";
import { DAYS, REASONS, formatLong, parseDate } from "../utils/dates";

export default function DoctorDetails() {
  const { doctorId } = useParams();
  const [doctor, setDoctor] = useState(null);
  const [days, setDays] = useState([]);
  const [date, setDate] = useState(null);
  const [day, setDay] = useState(null);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dayLoading, setDayLoading] = useState(false);

  useEffect(() => {
    const c = new AbortController();
    setLoading(true);
    Promise.all([getDoctor(doctorId, c.signal), getUpcoming(doctorId, 14, c.signal)])
      .then(([d, up]) => {
        setDoctor(d);
        setDays(up);
        const first = up.find((x) => x.slotCount > 0) || up[0];
        setDate(first ? first.date : null);
        setLoading(false);
      })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setLoading(false);
      });
    return () => c.abort();
  }, [doctorId]);

  useEffect(() => {
    if (!date) return;
    const c = new AbortController();
    setDayLoading(true);
    setSelected(null);
    getAvailability(doctorId, date, c.signal)
      .then((a) => { setDay(a); setDayLoading(false); })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setDayLoading(false);
      });
    return () => c.abort();
  }, [doctorId, date]);

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

        <h2 className="mt-8 text-sm font-semibold uppercase tracking-wide text-slate-500">Choose a date</h2>
        <div className="mt-3 flex gap-2 overflow-x-auto pb-2" role="group" aria-label="Choose a date">
          {days.map((x) => {
            const dt = parseDate(x.date);
            const active = x.date === date;
            return (
              <button key={x.date} type="button" onClick={() => setDate(x.date)} aria-pressed={active}
                      className={`min-w-[4.5rem] rounded-lg border px-3 py-2 text-center text-sm ${
                        active ? "border-brand-600 bg-brand-600 text-white"
                               : x.slotCount > 0 ? "border-slate-300 bg-white hover:bg-slate-100"
                                                 : "border-slate-200 bg-slate-50 text-slate-400"}`}>
                <span className="block text-xs">{dt.toLocaleDateString(undefined, { weekday: "short" })}</span>
                <span className="block text-lg font-semibold">{dt.getDate()}</span>
                <span className="block text-xs">{x.slotCount > 0 ? `${x.slotCount} slots` : "none"}</span>
              </button>
            );
          })}
        </div>

        <div className="mt-6" aria-live="polite">
          {date && <h3 className="font-semibold text-slate-800">{formatLong(date)}</h3>}
          {dayLoading && <p className="mt-2 text-slate-500">Loading slots...</p>}
          {!dayLoading && day && day.slots.length === 0 && (
            <p className="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">
              {REASONS[day.unavailableReason] || "No slots available."} Pick another date.
            </p>
          )}
          {!dayLoading && day && day.slots.length > 0 && (
            <ul className="mt-3 flex flex-wrap gap-2">
              {day.slots.map((s) => (
                <li key={s.start}>
                  <button type="button" onClick={() => setSelected(s.start)} aria-pressed={selected === s.start}
                          className={`rounded-lg border px-3 py-1.5 text-sm font-medium ${
                            selected === s.start ? "border-brand-600 bg-brand-600 text-white"
                                                 : "border-slate-300 bg-white hover:bg-slate-100"}`}>
                    {s.start}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <button type="button" disabled title="Booking opens in Phase 5"
                className="mt-6 rounded-lg border border-slate-300 px-5 py-2 font-medium text-slate-400">
          Book Appointment
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