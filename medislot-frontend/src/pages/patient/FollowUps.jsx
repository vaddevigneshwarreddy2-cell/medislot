import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getFollowUps } from "../../api/clinical";
import { getApiError } from "../../api/errors";
import { formatLong } from "../../utils/dates";

export default function FollowUps() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getFollowUps().then(setItems).catch((e) => setError(getApiError(e).message));
  }, []);

  return (
    <section className="mx-auto max-w-3xl px-4 py-10">
      <Link to="/patient" className="text-sm font-medium text-brand-600 hover:underline">&larr; Dashboard</Link>
      <h1 className="mt-2 text-3xl font-bold text-slate-900">Follow-ups</h1>
      <p className="mt-1 text-slate-600">Dates your doctor suggested for a return visit. A follow-up still has to be booked.</p>

      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      {!items && !error && <p className="p-8 text-center text-slate-500">Loading...</p>}
      {items && items.length === 0 && (
        <p className="mt-6 rounded-xl border border-dashed border-slate-300 p-10 text-center text-slate-600">
          You have no upcoming follow-ups.
        </p>
      )}

      <ul className="mt-6 space-y-3">
        {(items || []).map((f) => (
          <li key={f.appointmentId} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <div>
              <p className="font-semibold text-slate-900">{formatLong(f.followUpDate)}</p>
              <p className="text-sm text-slate-600">{f.doctorName} &middot; {f.specialization}</p>
              <p className="text-sm text-slate-500">{f.hospitalName}</p>
            </div>
            <Link to={`/doctors/${f.doctorId}`}
                  className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
              Book follow-up
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}