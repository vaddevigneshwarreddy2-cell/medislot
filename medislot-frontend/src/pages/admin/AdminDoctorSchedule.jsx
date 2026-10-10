import { Link, useParams } from "react-router-dom";
import ScheduleEditor from "../../components/ScheduleEditor";

export default function AdminDoctorSchedule() {
  const { doctorId } = useParams();
  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <Link to="/admin/doctors" className="text-sm font-medium text-brand-600 hover:underline">&larr; Doctors</Link>
      <h1 className="mt-2 mb-6 text-3xl font-bold text-slate-900">Doctor schedule and leave</h1>
      <ScheduleEditor basePath={`/admin/doctors/${doctorId}`} />
    </section>
  );
}