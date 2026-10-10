import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listPrescriptions } from "../../api/clinical";
import { getApiError } from "../../api/errors";
import PdfButton from "../../components/PdfButton";
import { formatLong } from "../../utils/dates";

// role: "PATIENT" or "DOCTOR"
export default function PrescriptionList({ role }) {
  const [items, setItems] = useState(null);
  const [error, setError] = useState(null);
  const doctor = role === "DOCTOR";
  const base = doctor ? "/doctor/prescriptions" : "/patient/prescriptions";

  useEffect(() => {
    listPrescriptions(role).then(setItems).catch((e) => setError(getApiError(e).message));
  }, [role]);

  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <Link to={doctor ? "/doctor" : "/patient"} className="text-sm font-medium text-brand-600 hover:underline">
        &larr; Dashboard
      </Link>
      <h1 className="mt-2 text-3xl font-bold text-slate-900">{doctor ? "Prescriptions I issued" : "My prescriptions"}</h1>

      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      {!items && !error && <p className="p-8 text-center text-slate-500">Loading...</p>}

      {items && items.length === 0 && (
        <p className="mt-6 rounded-xl border border-dashed border-slate-300 p-10 text-center text-slate-600">
          {doctor ? "You have not completed any consultations yet."
                  : "You have no prescriptions yet. They appear here after your doctor completes a consultation."}
        </p>
      )}

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        {(items || []).map((p) => (
          <article key={p.id} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">{formatLong(p.date)}</p>
            <h2 className="text-lg font-semibold text-slate-900">{doctor ? p.patientName : p.doctorName}</h2>
            {!doctor && <p className="text-sm text-slate-600">{p.specialization} &middot; {p.hospitalName}</p>}
            <p className="mt-2 text-sm text-slate-700">
              {p.medicineCount} medicine{p.medicineCount === 1 ? "" : "s"}
              {p.followUpDate ? ` - follow-up ${p.followUpDate}` : ""}
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              <Link to={`${base}/${p.id}`}
                    className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
                View
              </Link>
              <PdfButton role={role} id={p.id} />
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}