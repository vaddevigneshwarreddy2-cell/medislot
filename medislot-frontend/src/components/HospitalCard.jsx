import { Link } from "react-router-dom";
import { hasCoordinates, viewLocationUrl } from "../utils/maps";

export default function HospitalCard({ hospital, origin }) {
  const h = hospital;
  const shown = h.departments.slice(0, 4);
  const extra = h.departments.length - shown.length;

  return (
    <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-lg font-semibold text-slate-900">
        <span aria-hidden="true">&#127973;</span> {h.name}
      </h2>
      <p className="mt-2 text-sm text-slate-600">
        <span aria-hidden="true">&#128205;</span> {h.addressLine}, {h.city}
      </p>
      {h.distanceKm != null && (
        <p className="mt-1 text-sm font-medium text-brand-700">
          <span aria-hidden="true">&#128207;</span> {h.distanceKm} km away
        </p>
      )}

      {h.departments.length > 0 && (
        <p className="mt-3 text-sm text-slate-600">
          <span className="font-medium text-slate-700">Departments:</span> {shown.join(", ")}
          {extra > 0 ? ` +${extra} more` : ""}
        </p>
      )}
      <p className="mt-1 text-sm text-slate-600">
        {h.availableDoctors > 0
          ? `${h.availableDoctors} doctor${h.availableDoctors === 1 ? "" : "s"} available`
          : "No doctors listed yet"}
      </p>

      <div className="mt-4 flex flex-wrap gap-2">
        <Link
          to={`/hospitals/${h.id}`}
          state={{ origin }}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
        >
          View Hospital
        </Link>
        <Link
          to={`/doctors?hospitalId=${h.id}`}
          className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
        >
          Book Appointment
        </Link>
        {hasCoordinates(h) && (
          <a
            href={viewLocationUrl(h)}
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            View Location
          </a>
        )}
      </div>
    </article>
  );
}