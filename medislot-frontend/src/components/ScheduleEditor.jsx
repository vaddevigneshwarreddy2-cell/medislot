import { useEffect, useState } from "react";
import api from "../api/axios";
import { getApiError } from "../api/errors";
import { DAYS, formatLong, toMinutes, todayString } from "../utils/dates";

// basePath is "/doctor" (own schedule) or `/admin/doctors/${id}` (admin acting on a doctor).
function validate(windows) {
  for (const w of windows) {
    if (!w.start || !w.end) return "Fill in every start and end time.";
    if (toMinutes(w.end) <= toMinutes(w.start)) return `${DAYS[w.dayOfWeek - 1]}: end time must be after start time.`;
  }
  for (let day = 1; day <= 7; day++) {
    const list = windows.filter((w) => w.dayOfWeek === day).sort((a, b) => toMinutes(a.start) - toMinutes(b.start));
    if (list.length > 3) return `${DAYS[day - 1]}: at most 3 working windows per day.`;
    for (let i = 1; i < list.length; i++) {
      if (toMinutes(list[i].start) < toMinutes(list[i - 1].end)) return `${DAYS[day - 1]}: working windows must not overlap.`;
    }
  }
  return null;
}

export default function ScheduleEditor({ basePath }) {
  const [windows, setWindows] = useState([]);
  const [leaves, setLeaves] = useState([]);
  const [leaveForm, setLeaveForm] = useState({ leaveDate: "", reason: "" });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState(null);
  const [error, setError] = useState(null);
  const [leaveError, setLeaveError] = useState(null);

  async function load() {
    try {
      const [a, l] = await Promise.all([api.get(`${basePath}/availability`), api.get(`${basePath}/leave`)]);
      setWindows(a.data.data);
      setLeaves(l.data.data);
      setError(null);
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { setLoading(true); load(); /* eslint-disable-next-line */ }, [basePath]);

  const update = (idx, key, value) =>
    setWindows((ws) => ws.map((w, i) => (i === idx ? { ...w, [key]: value } : w)));

  async function save() {
    setMsg(null);
    const problem = validate(windows);
    if (problem) { setError(problem); return; }
    setSaving(true);
    try {
      const res = await api.put(`${basePath}/availability`, { windows });
      setWindows(res.data.data);
      setError(null);
      setMsg("Working hours saved.");
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setSaving(false);
    }
  }

  async function addLeave(e) {
    e.preventDefault();
    setLeaveError(null);
    if (!leaveForm.leaveDate) { setLeaveError("Choose a date."); return; }
    try {
      await api.post(`${basePath}/leave`, { leaveDate: leaveForm.leaveDate, reason: leaveForm.reason });
      setLeaveForm({ leaveDate: "", reason: "" });
      const l = await api.get(`${basePath}/leave`);
      setLeaves(l.data.data);
    } catch (err) {
      setLeaveError(getApiError(err).message);
    }
  }

  async function removeLeave(id) {
    if (!window.confirm("Remove this leave day?")) return;
    try {
      await api.delete(`${basePath}/leave/${id}`);
      setLeaves((ls) => ls.filter((x) => x.id !== id));
    } catch (err) {
      setLeaveError(getApiError(err).message);
    }
  }

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;

  const inputCls = "rounded-lg border border-slate-300 px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500";

  return (
    <div className="space-y-8">
      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Weekly working hours</h2>
        <p className="text-sm text-slate-500">Slots are generated from these hours using the appointment length.</p>

        {error && <p role="alert" className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        {msg && <p role="status" className="mt-3 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}

        <div className="mt-4 space-y-4">
          {DAYS.map((name, i) => {
            const day = i + 1;
            const rows = windows.map((w, idx) => ({ w, idx })).filter((r) => r.w.dayOfWeek === day);
            return (
              <div key={name} className="flex flex-wrap items-start gap-3 border-b border-slate-100 pb-3">
                <span className="w-28 pt-1.5 text-sm font-medium text-slate-700">{name}</span>
                <div className="flex-1 space-y-2">
                  {rows.length === 0 && <span className="text-sm text-slate-400">Not working</span>}
                  {rows.map(({ w, idx }) => (
                    <div key={idx} className="flex flex-wrap items-center gap-2">
                      <input type="time" aria-label={`${name} start`} className={inputCls} value={w.start}
                             onChange={(e) => update(idx, "start", e.target.value)} />
                      <span className="text-slate-400">to</span>
                      <input type="time" aria-label={`${name} end`} className={inputCls} value={w.end}
                             onChange={(e) => update(idx, "end", e.target.value)} />
                      <button type="button" onClick={() => setWindows((ws) => ws.filter((_, j) => j !== idx))}
                              className="text-sm font-medium text-red-600 hover:underline">Remove</button>
                    </div>
                  ))}
                </div>
                <button type="button"
                        onClick={() => setWindows((ws) => [...ws, { dayOfWeek: day, start: "10:00", end: "13:00" }])}
                        className="text-sm font-medium text-brand-600 hover:underline">+ Add hours</button>
              </div>
            );
          })}
        </div>

        <button type="button" onClick={save} disabled={saving}
                className="mt-5 rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
          {saving ? "Saving..." : "Save working hours"}
        </button>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Leave</h2>
        <p className="text-sm text-slate-500">Slots on a leave day are not offered to patients.</p>

        {leaveError && <p role="alert" className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{leaveError}</p>}

        <form onSubmit={addLeave} className="mt-4 flex flex-wrap items-end gap-3">
          <div>
            <label htmlFor="leave-date" className="block text-sm font-medium text-slate-700">Date</label>
            <input id="leave-date" type="date" min={todayString()} className={inputCls + " mt-1"}
                   value={leaveForm.leaveDate} onChange={(e) => setLeaveForm({ ...leaveForm, leaveDate: e.target.value })} />
          </div>
          <div className="flex-1">
            <label htmlFor="leave-reason" className="block text-sm font-medium text-slate-700">Reason (optional)</label>
            <input id="leave-reason" maxLength={255} className={inputCls + " mt-1 w-full"}
                   value={leaveForm.reason} onChange={(e) => setLeaveForm({ ...leaveForm, reason: e.target.value })} />
          </div>
          <button type="submit" className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium hover:bg-slate-100">
            Add leave
          </button>
        </form>

        {leaves.length === 0 ? (
          <p className="mt-4 text-sm text-slate-500">No upcoming leave.</p>
        ) : (
          <ul className="mt-4 divide-y divide-slate-100">
            {leaves.map((l) => (
              <li key={l.id} className="flex items-center justify-between py-2 text-sm">
                <span>
                  <span className="font-medium text-slate-800">{formatLong(l.leaveDate)}</span>
                  {l.reason ? <span className="text-slate-500"> - {l.reason}</span> : null}
                </span>
                <button type="button" onClick={() => removeLeave(l.id)} className="font-medium text-red-600 hover:underline">
                  Remove
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}