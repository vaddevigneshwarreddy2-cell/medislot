import { useEffect, useState } from "react";
import ReceptionPage from "../../components/ReceptionPage";
import StatusBadge from "../../components/StatusBadge";
import usePolling from "../../hooks/usePolling";
import { changeStatus, checkInById, recAppointments, recDoctors } from "../../api/staff";
import { getApiError } from "../../api/errors";
import { todayString } from "../../utils/dates";
import { to12h } from "../../utils/format";

const STATUSES = ["CONFIRMED", "ATTENDANCE_PENDING", "CHECKED_IN", "WAITING", "IN_CONSULTATION", "COMPLETED", "CANCELLED", "NO_SHOW", "RESCHEDULED"];

export default function ReceptionAppointments() {
  const [doctors, setDoctors] = useState([]);
  const [date, setDate] = useState(todayString());
  const [doctorId, setDoctorId] = useState("");
  const [status, setStatus] = useState("");
  const [qInput, setQInput] = useState("");
  const [q, setQ] = useState("");
  const [rows, setRows] = useState(null);
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);
  const [reload, setReload] = useState(0);

  useEffect(() => { recDoctors().then(setDoctors).catch(() => {}); }, []);

  usePolling(() => {
    const params = {};
    if (date) params.date = date;
    if (doctorId) params.doctorId = doctorId;
    if (status) params.status = status;
    if (q) params.q = q;
    recAppointments(params).then((r) => { setRows(r); setError(null); }).catch((e) => setError(getApiError(e).message));
  }, 20000, [date, doctorId, status, q, reload]);

  async function act(fn, success) {
    setError(null);
    setMsg(null);
    try {
      await fn();
      setMsg(success);
      setReload((n) => n + 1);
    } catch (err) {
      setError(getApiError(err).message);
    }
  }

  const checkIn = (a) => act(() => checkInById(a.appointmentId), `${a.patientName} checked in.`);
  const cancel = (a) => {
    if (window.confirm(`Cancel the appointment of ${a.patientName}?`))
      act(() => changeStatus(a.appointmentId, "CANCELLED", "Cancelled by reception"), "Appointment cancelled.");
  };
  const noShow = (a) => {
    if (window.confirm(`Mark ${a.patientName} as a no-show?`))
      act(() => changeStatus(a.appointmentId, "NO_SHOW", "Did not attend"), "Marked as no-show.");
  };

  const isToday = date === todayString();
  const inputCls = "rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500";

  return (
    <ReceptionPage title="Appointments">
      <form onSubmit={(e) => { e.preventDefault(); setQ(qInput.trim()); }} className="flex flex-wrap items-end gap-3">
        <div><label htmlFor="a-date" className="block text-xs font-medium text-slate-600">Date</label>
          <input id="a-date" type="date" className={inputCls} value={date} onChange={(e) => setDate(e.target.value)} /></div>
        <div><label htmlFor="a-doc" className="block text-xs font-medium text-slate-600">Doctor</label>
          <select id="a-doc" className={inputCls} value={doctorId} onChange={(e) => setDoctorId(e.target.value)}>
            <option value="">All</option>{doctors.map((d) => <option key={d.id} value={d.id}>{d.fullName}</option>)}
          </select></div>
        <div><label htmlFor="a-status" className="block text-xs font-medium text-slate-600">Status</label>
          <select id="a-status" className={inputCls} value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Any</option>{STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select></div>
        <div className="flex-1"><label htmlFor="a-q" className="block text-xs font-medium text-slate-600">Patient name or appointment ID</label>
          <input id="a-q" className={inputCls + " w-full"} value={qInput} maxLength={100} onChange={(e) => setQInput(e.target.value)} /></div>
        <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">Search</button>
      </form>
      <p className="mt-2 text-xs text-slate-500">Searching by appointment ID ignores the date filter.</p>

      <div aria-live="polite">
        {msg && <p role="status" className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}
        {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      </div>

      <div className="mt-4 overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-3">Time</th><th className="px-4 py-3">Patient</th><th className="px-4 py-3">Doctor</th>
                <th className="px-4 py-3">Status</th><th className="px-4 py-3" /></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {rows && rows.length === 0 && (
              <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-500">No appointments found.</td></tr>
            )}
            {(rows || []).map((a) => {
              const arrivable = a.status === "CONFIRMED" || a.status === "ATTENDANCE_PENDING";
              return (
                <tr key={a.appointmentId}>
                  <td className="px-4 py-3 whitespace-nowrap">{to12h(a.appointmentTime)}
                    <span className="block text-xs text-slate-400">#{a.appointmentId}{a.source === "WALK_IN" ? " - walk-in" : ""}</span></td>
                  <td className="px-4 py-3"><div className="font-medium text-slate-800">{a.patientName}</div>
                    <div className="text-xs text-slate-500">{a.patientMobile}</div></td>
                  <td className="px-4 py-3">{a.doctorName}</td>
                  <td className="px-4 py-3"><StatusBadge status={a.status} />
                    {a.queueNumber != null && <span className="ml-2 text-xs text-slate-500">#{a.queueNumber}</span>}</td>
                  <td className="space-x-3 px-4 py-3 text-right whitespace-nowrap">
                    {arrivable && isToday && (
                      <button type="button" onClick={() => checkIn(a)} className="font-medium text-brand-600 hover:underline">Check in</button>)}
                    {(arrivable || a.status === "WAITING") && (
                      <button type="button" onClick={() => noShow(a)} className="font-medium text-slate-600 hover:underline">No-show</button>)}
                    {(arrivable || a.status === "CHECKED_IN" || a.status === "WAITING") && (
                      <button type="button" onClick={() => cancel(a)} className="font-medium text-red-600 hover:underline">Cancel</button>)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </ReceptionPage>
  );
}