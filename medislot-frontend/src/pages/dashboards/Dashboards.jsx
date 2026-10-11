import { useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/axios";
import { getApiError } from "../../api/errors";
import { useAuth } from "../../auth/AuthContext";
import usePolling from "../../hooks/usePolling";
import { formatLong } from "../../utils/dates";
import { to12h } from "../../utils/format";

function useLoad(path) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  usePolling(() => {
    api.get(path).then((r) => { setData(r.data.data); setError(null); })
      .catch((e) => setError(getApiError(e).message));
  }, 30000, [path]);
  return { data, error };
}

function Layout({ title, error, children }) {
  const { user } = useAuth();
  return (
    <section className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-3xl font-bold text-slate-900">{title}</h1>
      <p className="mt-1 text-slate-600">
        Signed in as <span className="font-semibold">{user.fullName}</span>{" "}
        <span className="ml-1 rounded-full bg-brand-100 px-2 py-0.5 text-xs font-semibold text-brand-700">{user.role}</span>
      </p>
      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      {children}
    </section>
  );
}

export function Stats({ items }) {
  return (
    <div className="mt-6 grid gap-3 sm:grid-cols-3 lg:grid-cols-6">
      {items.map(([label, value]) => (
        <div key={label} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <p className="text-3xl font-bold text-slate-900">{value ?? "-"}</p>
          <p className="text-sm text-slate-500">{label}</p>
        </div>
      ))}
    </div>
  );
}

function Cards({ items }) {
  return (
    <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {items.map((i) => (
        <Link key={i.label} to={i.to} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm hover:border-brand-500">
          <h2 className="font-semibold text-slate-800">{i.label}</h2>
          <p className="mt-1 text-sm text-slate-500">{i.note}</p>
        </Link>
      ))}
    </div>
  );
}

export function PatientDashboard() {
  const { data: d, error } = useLoad("/patient/dashboard");
  return (
    <Layout title="Patient dashboard" error={error}>
      {d && (
        <>
          <Stats items={[["Upcoming", d.upcoming], ["Completed", d.completed], ["Cancelled", d.cancelled],
                         ["No-shows", d.noShows], ["Follow-ups", d.followUps], ["Unread notifications", d.unreadNotifications]]} />
          <h2 className="mt-8 text-sm font-semibold uppercase tracking-wide text-slate-500">Next appointment</h2>
          {d.next ? (
            <Link to={`/appointments/${d.next.appointmentId}`}
                  className="mt-2 block rounded-xl border border-slate-200 bg-white p-5 shadow-sm hover:border-brand-500">
              <p className="text-lg font-semibold text-slate-900">{d.next.doctorName}</p>
              <p className="text-sm text-slate-600">{d.next.hospitalName}</p>
              <p className="mt-1 font-medium text-slate-800">{formatLong(d.next.date)} at {to12h(d.next.time)}</p>
            </Link>
          ) : (
            <p className="mt-2 text-sm text-slate-500">
              You have nothing booked. <Link to="/doctors" className="font-medium text-brand-600 hover:underline">Find a doctor</Link>.
            </p>
          )}
        </>
      )}
      <Cards items={[
        { label: "Find a hospital", note: "Search near you or by name", to: "/hospitals" },
        { label: "Find a doctor", note: "By specialization, name or location", to: "/doctors" },
        { label: "My appointments", note: "Upcoming and past visits, QR codes", to: "/patient/appointments" },
        { label: "Prescriptions", note: "View and download after a visit", to: "/patient/prescriptions" },
        { label: "Follow-ups", note: "Return visits your doctor suggested", to: "/patient/follow-ups" },
        { label: "Notifications", note: "Reminders and updates", to: "/notifications" },
      ]} />
    </Layout>
  );
}

export function DoctorDashboard() {
  const { data: d, error } = useLoad("/doctor/dashboard");
  return (
    <Layout title="Doctor dashboard" error={error}>
      {d && (
        <>
          <p className="mt-4 text-sm text-slate-500">Today, {formatLong(d.date)}</p>
          <Stats items={[["Booked today", d.total], ["Expected", d.expected], ["Waiting", d.waiting],
                         ["In consultation", d.inConsultation], ["Completed", d.completed], ["Later bookings", d.upcomingLater]]} />
        </>
      )}
      <Cards items={[
        { label: "Today's queue", note: "Call, start and complete consultations", to: "/doctor/queue" },
        { label: "Prescriptions issued", note: "Completed consultations", to: "/doctor/prescriptions" },
        { label: "Availability and leave", note: "Working hours and days off", to: "/doctor/schedule" },
      ]} />
    </Layout>
  );
}

export function ReceptionDashboard() {
  return (
    <Layout title="Reception dashboard">
      <Cards items={[
        { label: "Reception home", note: "Today at a glance", to: "/reception" },
        { label: "QR check-in", note: "Scan or enter a code", to: "/reception/check-in" },
        { label: "Queue board", note: "Who is waiting", to: "/reception/queue" },
      ]} />
    </Layout>
  );
}

function Bar({ value, max }) {
  const pct = max === 0 ? 0 : Math.round((value / max) * 100);
  return (
    <div className="h-2 w-full rounded bg-slate-100" aria-hidden="true">
      <div className="h-2 rounded bg-brand-500" style={{ width: `${pct}%` }} />
    </div>
  );
}

function StatTable({ title, rows }) {
  const max = Math.max(0, ...rows.map((r) => r.booked));
  return (
    <div className="mt-8">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">{title}</h2>
      <div className="mt-2 overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-3">Name</th><th className="px-4 py-3 w-40">Booked</th><th className="px-4 py-3">Completed</th>
                <th className="px-4 py-3">Cancelled</th><th className="px-4 py-3">No-show</th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {rows.length === 0 && <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-500">No appointments in this period.</td></tr>}
            {rows.map((r) => (
              <tr key={r.name}>
                <td className="px-4 py-3 font-medium text-slate-800">{r.name}</td>
                <td className="px-4 py-3"><span className="mr-2">{r.booked}</span><Bar value={r.booked} max={max} /></td>
                <td className="px-4 py-3">{r.completed}</td><td className="px-4 py-3">{r.cancelled}</td><td className="px-4 py-3">{r.noShow}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export function AdminDashboard() {
  const [period, setPeriod] = useState("today");
  const { data: d, error } = useLoad(`/admin/dashboard?period=${period}`);
  const tab = (value, label) => (
    <button type="button" onClick={() => setPeriod(value)} aria-pressed={period === value}
            className={`rounded-full border px-4 py-1.5 text-sm font-medium ${
              period === value ? "border-brand-600 bg-brand-600 text-white" : "border-slate-300 bg-white hover:bg-slate-100"}`}>
      {label}
    </button>
  );
  return (
    <Layout title="Hospital admin dashboard" error={error}>
      <div className="mt-4 flex gap-2" role="group" aria-label="Period">
        {tab("today", "Today")}{tab("week", "Last 7 days")}{tab("month", "Last 30 days")}
      </div>
      {d && (
        <>
          <p className="mt-2 text-xs text-slate-500">{d.from} to {d.to}. Held, expired and replaced bookings are not counted.</p>
          <Stats items={[["Appointments", d.total], ["Completed", d.completed], ["Cancelled", d.cancelled],
                         ["No-shows", d.noShow], ["Checked in", d.checkedIn], ["Cancellation rate", `${d.cancellationRate}%`]]} />
          <p className="mt-2 text-sm text-slate-600">{d.activeDoctors} active doctor{d.activeDoctors === 1 ? "" : "s"}.</p>
          <StatTable title="By doctor" rows={d.doctors} />
          <StatTable title="By department" rows={d.departments} />
        </>
      )}
      <Cards items={[
        { label: "Doctors", note: "Add doctors, set schedules and leave", to: "/admin/doctors" },
        { label: "Departments", note: "Manage departments", to: "/admin/departments" },
        { label: "Holidays", note: "Days the hospital is closed", to: "/admin/holidays" },
      ]} />
    </Layout>
  );
}