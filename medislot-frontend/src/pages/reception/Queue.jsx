import { useEffect, useState } from "react";
import ReceptionPage from "../../components/ReceptionPage";
import StatusBadge from "../../components/StatusBadge";
import usePolling from "../../hooks/usePolling";
import { recDoctors, recQueue } from "../../api/staff";
import { getApiError } from "../../api/errors";
import { to12h } from "../../utils/format";

export default function ReceptionQueue() {
  const [doctors, setDoctors] = useState([]);
  const [doctorId, setDoctorId] = useState("");
  const [rows, setRows] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => { recDoctors().then(setDoctors).catch(() => {}); }, []);

  usePolling(() => {
    recQueue(doctorId || null).then((r) => { setRows(r); setError(null); }).catch((e) => setError(getApiError(e).message));
  }, 15000, [doctorId]);

  const groups = {};
  (rows || []).forEach((r) => { (groups[r.doctorId] ||= { name: r.doctorName, spec: r.specialization, items: [] }).items.push(r); });
  Object.values(groups).forEach((g) =>
    g.items.sort((a, b) => (a.status === "IN_CONSULTATION" ? -1 : 0) - (b.status === "IN_CONSULTATION" ? -1 : 0)));

  return (
    <ReceptionPage title="Queue board">
      <div className="flex items-center gap-3">
        <label htmlFor="q-doc" className="text-sm font-medium text-slate-700">Doctor</label>
        <select id="q-doc" value={doctorId} onChange={(e) => setDoctorId(e.target.value)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
          <option value="">All doctors</option>
          {doctors.map((d) => <option key={d.id} value={d.id}>{d.fullName}</option>)}
        </select>
        <span className="text-xs text-slate-500">Refreshes every 15 seconds</span>
      </div>

      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      {rows && rows.length === 0 && (
        <p className="mt-6 rounded-xl border border-dashed border-slate-300 p-8 text-center text-slate-500">
          Nobody is in the queue right now.
        </p>
      )}

      <div className="mt-6 grid gap-4 md:grid-cols-2">
        {Object.entries(groups).map(([id, g]) => (
          <div key={id} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="font-semibold text-slate-900">{g.name}</h2>
            <p className="text-sm text-slate-500">{g.spec}</p>
            <ul className="mt-3 divide-y divide-slate-100">
              {g.items.map((r) => (
                <li key={r.appointmentId} className="flex items-center justify-between gap-3 py-2 text-sm">
                  <div>
                    <p className="font-medium text-slate-800">
                      {r.queueNumber != null && <span className="mr-2 text-slate-500">#{r.queueNumber}</span>}
                      {r.patientName}
                    </p>
                    <p className="text-xs text-slate-500">
                      Slot {to12h(r.appointmentTime)}
                      {r.position != null && r.status !== "IN_CONSULTATION" ? ` - position ${r.position}` : ""}
                      {r.called ? " - called" : ""}
                    </p>
                  </div>
                  <StatusBadge status={r.status} />
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </ReceptionPage>
  );
}