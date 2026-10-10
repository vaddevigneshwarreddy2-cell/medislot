import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getPrescription } from "../../api/clinical";
import { getApiError } from "../../api/errors";
import PrescriptionView from "../../components/PrescriptionView";

export default function PrescriptionDetailPage({ role }) {
  const { prescriptionId } = useParams();
  const [rx, setRx] = useState(null);
  const [error, setError] = useState(null);
  const back = role === "DOCTOR" ? "/doctor/prescriptions" : "/patient/prescriptions";

  useEffect(() => {
    setRx(null);
    setError(null);
    getPrescription(role, prescriptionId).then(setRx).catch((e) => setError(getApiError(e).message));
  }, [role, prescriptionId]);

  return (
    <section className="mx-auto max-w-3xl px-4 py-10">
      <Link to={back} className="text-sm font-medium text-brand-600 hover:underline">&larr; Prescriptions</Link>
      <div className="mt-4">
        {error && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-red-700">{error}</p>}
        {!rx && !error && <p className="p-8 text-center text-slate-500">Loading...</p>}
        {rx && <PrescriptionView rx={rx} role={role} />}
      </div>
    </section>
  );
}