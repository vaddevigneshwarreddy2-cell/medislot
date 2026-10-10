import { Link } from "react-router-dom";
import ScheduleEditor from "../../components/ScheduleEditor";

export default function DoctorSchedule() {
  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <Link to="/doctor" className="text-sm font-medium text-brand-600 hover:underline">&larr; Dashboard</Link>
      <h1 className="mt-2 mb-6 text-3xl font-bold text-slate-900">My availability and leave</h1>
      <ScheduleEditor basePath="/doctor" />
    </section>
  );
}