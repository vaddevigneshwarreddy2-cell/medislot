import { Link } from "react-router-dom";
import StatusBadge from "./StatusBadge";
import { directionsUrl, hasCoordinates } from "../utils/maps";
import { formatLong } from "../utils/dates";
import { ACTIVE_STATUSES, to12h } from "../utils/format";

export default function AppointmentCard({ appt, onCancel }) {
  const a = appt;
  const h = a.hospital;
  const active = ACTIVE_STATUSES.includes(a.status);
  return (
    <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">{a.doctorName}</h2>
          <p className="text-sm text-slate-600">{a.specialization} &middot; {a.department}</p>
        </div>
        <StatusBadge status={a.status} />
      </div>
      {h && (
        <p className="mt-3 text-sm text-slate-700">
          <span aria-hidden="true">&#127973;</span> {h.name}
          <span className="block text-slate-500">{h.address}</span>
        </p>
      )}
      <p className="mt-2 text-sm font-medium text-slate-800">
        <span aria-hidden="true">&#128197;</span> {formatLong(a.appointmentDate)}{" "}
        <span aria-hidden="true">&#128336;</span> {to12h(a.appointmentTime)}
      </p>
      <div className="mt-4 flex flex-wrap gap-2">
        <Link to={`/appointments/${a.appointmentId}`}
              className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
          View appointment
        </Link>
        {active && hasCoordinates(h) && (
          <a href={directionsUrl(h)} target="_blank" rel="noopener noreferrer"
             className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100">
            <span aria-hidden="true">&#129517;</span> Get Directions
          </a>
        )}
        {a.canCancel && onCancel && (
          <button type="button" onClick={() => onCancel(a)}
                  className="rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50">
            Cancel
          </button>
        )}
      </div>
    </article>
  );
}