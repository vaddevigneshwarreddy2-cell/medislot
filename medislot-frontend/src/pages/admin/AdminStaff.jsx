import { Link } from "react-router-dom";
import StaffManager from "../../components/StaffManager";

export default function AdminStaff() {
  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <Link to="/admin" className="text-sm font-medium text-brand-600 hover:underline">&larr; Dashboard</Link>
      <h1 className="mt-2 text-3xl font-bold text-slate-900">Receptionists</h1>
      <p className="text-slate-600">Reception staff can check patients in, manage the queue and register walk-ins at your hospital.</p>
      <StaffManager basePath="/admin/staff" noun="Receptionist" />
    </section>
  );
}