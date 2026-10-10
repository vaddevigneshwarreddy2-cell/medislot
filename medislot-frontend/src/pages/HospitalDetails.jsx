import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { isCancel } from "axios";
import { getHospital } from "../api/hospitals";
import { getApiError } from "../api/errors";
import { directionsUrl, hasCoordinates, viewLocationUrl } from "../utils/maps";

export default function HospitalDetails() {
  const { hospitalId } = useParams();
  const location = useLocation();
  const origin = location.state?.origin || null; // passed in memory, never stored
  const [hospital, setHospital] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    getHospital(hospitalId, origin, controller.signal)
      .then((h) => { setHospital(h); setLoading(false); })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setLoading(false);
      });
    return () => controller.abort();
  }, [hospitalId, origin]);

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;

  if (error || !hospital) {
    return (
      <section className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p role="alert" className="text-lg text-slate-700">{error || "Hospital not found."}</p>
        <Link to="/hospitals" className="mt-4 inline-block font-medium text-brand-600 hover:underline">
          Back to hospital search
        </Link>
      </section>
    );
  }

  const h = hospital;
  const canMap = hasCoordinates(h);

  return (
    <section className="mx-auto max-w-3xl px-4 py-10">
      <Link to="/hospitals" className="text-sm font-medium text-brand-600 hover:underline">
        &larr; Back to search
      </Link>

      <div className="mt-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="text-3xl font-bold text-slate-900">
          <span aria-hidden="true">&#127973;</span> {h.name}
        </h1>
        <p className="mt-3 text-slate-700">
          <span aria-hidden="true">&#128205;</span> {h.addressLine}, {h.city}, {h.state}
          {h.postalCode ? ` ${h.postalCode}` : ""}
        </p>
        {h.distanceKm != null && (
          <p className="mt-1 font-medium text-brand-700">{h.distanceKm} km away</p>
        )}
        {h.phone && (
          <p className="mt-1 text-sm text-slate-600">
            Phone: <a href={`tel:${h.phone}`} className="text-brand-600 hover:underline">{h.phone}</a>
          </p>
        )}
        <p className="mt-1 text-sm text-slate-600">
          {h.availableDoctors > 0
            ? `${h.availableDoctors} doctor${h.availableDoctors === 1 ? "" : "s"} available`
            : "No doctors listed yet"}
        </p>

        <h2 className="mt-6 text-sm font-semibold uppercase tracking-wide text-slate-500">Departments</h2>
        {h.departments.length > 0 ? (
          <ul className="mt-2 flex flex-wrap gap-2">
            {h.departments.map((d) => (
              <li key={d.id} className="rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-700">{d.name}</li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-sm text-slate-500">No departments listed yet.</p>
        )}

        <div className="mt-8 flex flex-wrap gap-3">
          <Link
            to={`/doctors?hospitalId=${h.id}`}
            className="rounded-lg border border-brand-600 px-5 py-2 font-medium text-brand-600 hover:bg-brand-50"
          >
            Find doctors here
          </Link>
          <Link
            to={`/doctors?hospitalId=${h.id}`}
            className="rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700"
          >
            Book Appointment
          </Link>
          {canMap ? (
            <>
              <a
                href={viewLocationUrl(h)}
                target="_blank"
                rel="noopener noreferrer"
                className="rounded-lg border border-slate-300 px-5 py-2 font-medium text-slate-700 hover:bg-slate-100"
              >
                View Location
              </a>
              <a
                href={directionsUrl(h)}
                target="_blank"
                rel="noopener noreferrer"
                className="rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700"
              >
                <span aria-hidden="true">&#129517;</span> Get Directions in Google Maps
              </a>
            </>
          ) : (
            <p className="text-sm text-slate-500">This hospital has no map location yet.</p>
          )}
        </div>
      </div>
    </section>
  );
}