import { useEffect, useState } from "react";
import { isCancel } from "axios";
import { getAvailability, getUpcoming } from "../api/doctors";
import { getApiError } from "../api/errors";
import { REASONS, formatLong, parseDate } from "../utils/dates";
import { to12h } from "../utils/format";

const MESSAGES = { ...REASONS, FULLY_BOOKED: "Every slot on this day is booked." };

// selected: { date, time } or null. Change refreshKey to reload after a failed booking.
export default function SlotPicker({ doctorId, selected, onSelect, refreshKey = 0 }) {
  const [days, setDays] = useState([]);
  const [date, setDate] = useState(null);
  const [day, setDay] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dayLoading, setDayLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const c = new AbortController();
    setLoading(true);
    getUpcoming(doctorId, 14, c.signal)
      .then((up) => {
        setDays(up);
        setDate((prev) => {
          if (prev && up.some((x) => x.date === prev)) return prev;
          const first = up.find((x) => x.slotCount > 0) || up[0];
          return first ? first.date : null;
        });
        setError(null);
        setLoading(false);
      })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setLoading(false);
      });
    return () => c.abort();
  }, [doctorId, refreshKey]);

  useEffect(() => {
    if (!date) return;
    const c = new AbortController();
    setDayLoading(true);
    getAvailability(doctorId, date, c.signal)
      .then((a) => { setDay(a); setDayLoading(false); })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setDayLoading(false);
      });
    return () => c.abort();
  }, [doctorId, date, refreshKey]);

  if (loading) return <p className="text-slate-500">Loading availability...</p>;
  if (error) return <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>;

  return (
    <div>
      <div className="flex gap-2 overflow-x-auto pb-2" role="group" aria-label="Choose a date">
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

      <div className="mt-4" aria-live="polite">
        {date && <h3 className="font-semibold text-slate-800">{formatLong(date)}</h3>}
        {dayLoading && <p className="mt-2 text-slate-500">Loading slots...</p>}
        {!dayLoading && day && day.slots.length === 0 && (
          <p className="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">
            {MESSAGES[day.unavailableReason] || "No slots available."} Pick another date.
          </p>
        )}
        {!dayLoading && day && day.slots.length > 0 && (
          <ul className="mt-3 flex flex-wrap gap-2">
            {day.slots.map((s) => {
              const isSel = selected && selected.date === date && selected.time === s.start;
              return (
                <li key={s.start}>
                  <button type="button" onClick={() => onSelect({ date, time: s.start })} aria-pressed={Boolean(isSel)}
                          className={`rounded-lg border px-3 py-1.5 text-sm font-medium ${
                            isSel ? "border-brand-600 bg-brand-600 text-white"
                                  : "border-slate-300 bg-white hover:bg-slate-100"}`}>
                    {to12h(s.start)}
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}