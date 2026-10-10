import { Link } from "react-router-dom";

export default function DoctorCard({ doctor }) {
  const d = doctor;
  return (
    <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-lg font-semibold text-slate-900">{d.fullName}</h2>
      <p className="text-sm font-medium text-brand-700">{d.specialization}</p>
      {d.qualification && <p className="mt-1 text-sm text-slate-600">{d.qualification}</p>}
      {d.experienceYears != null && (
        <p className="text-sm text-slate-600">{d.experienceYears} years of experience</p>
      )}
      <p className="mt-3 text-sm text-slate-700">
        <span aria-hidden="true">&#127973;</span> {d.hospitalName}, {d.hospitalCity}
      </p>
      <p className="text-sm text-slate-600">Department: {d.departmentName}</p>
      {d.distanceKm != null && (
        <p className="text-sm font-medium text-brand-700">{d.distanceKm} km away</p>
      )}
      <Link
        to={`/doctors/${d.id}`}
        className="mt-4 inline-block rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
      >
        View profile and slots
      </Link>
    </article>
  );
}