import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/axios";
import { getApiError } from "../../api/errors";
import Field from "../../components/Field";

const EMPTY = { fullName: "", email: "", mobile: "", password: "", departmentId: "",
                specialization: "", qualification: "", experienceYears: "", consultationDurationMin: "30" };

export default function AdminDoctors() {
  const [doctors, setDoctors] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function load() {
    try {
      const [d, dep] = await Promise.all([api.get("/admin/doctors"), api.get("/admin/departments")]);
      setDoctors(d.data.data);
      setDepartments(dep.data.data.filter((x) => x.active));
      setError(null);
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => { load(); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  function validate() {
    const e = {};
    if (!form.fullName.trim()) e.fullName = "Enter the doctor's name.";
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) e.email = "Enter a valid email.";
    if (form.mobile && !/^\+?[0-9]{10,15}$/.test(form.mobile.trim())) e.mobile = "Use 10-15 digits.";
    if (form.password.length < 8 || !/[a-z]/.test(form.password) || !/[A-Z]/.test(form.password) || !/\d/.test(form.password))
      e.password = "At least 8 characters with upper case, lower case and a digit.";
    if (!form.departmentId) e.departmentId = "Choose a department.";
    if (!form.specialization.trim()) e.specialization = "Enter a specialization.";
    const mins = Number(form.consultationDurationMin);
    if (!Number.isInteger(mins) || mins < 5 || mins > 240) e.consultationDurationMin = "5 to 240 minutes.";
    return e;
  }

  async function submit(e) {
    e.preventDefault();
    setMsg(null);
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length) return;
    setSaving(true);
    try {
      const body = {
        fullName: form.fullName.trim(), email: form.email.trim(), password: form.password,
        departmentId: Number(form.departmentId), specialization: form.specialization.trim(),
        consultationDurationMin: Number(form.consultationDurationMin),
      };
      if (form.mobile.trim()) body.mobile = form.mobile.trim();
      if (form.qualification.trim()) body.qualification = form.qualification.trim();
      if (form.experienceYears !== "") body.experienceYears = Number(form.experienceYears);
      await api.post("/admin/doctors", body);
      setForm(EMPTY);
      setMsg("Doctor created. Share the initial password with them securely.");
      setError(null);
      await load();
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setSaving(false);
    }
  }

  async function toggle(d) {
    try {
      await api.patch(`/admin/doctors/${d.id}/active`, { active: !d.active });
      await load();
    } catch (err) {
      setError(getApiError(err).message);
    }
  }

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;

  return (
    <section className="mx-auto max-w-5xl px-4 py-10">
      <Link to="/admin" className="text-sm font-medium text-brand-600 hover:underline">&larr; Dashboard</Link>
      <h1 className="mt-2 text-3xl font-bold text-slate-900">Doctors</h1>

      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      {msg && <p role="status" className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}

      <div className="mt-6 overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-3">Name</th><th className="px-4 py-3">Specialization</th>
              <th className="px-4 py-3">Department</th><th className="px-4 py-3">Status</th><th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {doctors.length === 0 && (
              <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-500">No doctors yet.</td></tr>
            )}
            {doctors.map((d) => (
              <tr key={d.id}>
                <td className="px-4 py-3"><div className="font-medium text-slate-800">{d.fullName}</div>
                  <div className="text-xs text-slate-500">{d.email}</div></td>
                <td className="px-4 py-3">{d.specialization}</td>
                <td className="px-4 py-3">{d.departmentName}</td>
                <td className="px-4 py-3">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${d.active ? "bg-green-100 text-green-700" : "bg-slate-200 text-slate-600"}`}>
                    {d.active ? "Active" : "Disabled"}
                  </span>
                </td>
                <td className="space-x-3 px-4 py-3 text-right">
                  <Link to={`/admin/doctors/${d.id}/schedule`} className="font-medium text-brand-600 hover:underline">Schedule</Link>
                  <button type="button" onClick={() => toggle(d)} className="font-medium text-slate-600 hover:underline">
                    {d.active ? "Disable" : "Enable"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form onSubmit={submit} noValidate className="mt-8 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Add a doctor</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <Field id="fullName" label="Full name" value={form.fullName} onChange={set("fullName")} error={errors.fullName} />
          <Field id="email" label="Email" type="email" value={form.email} onChange={set("email")} error={errors.email} />
          <Field id="mobile" label="Mobile (optional)" type="tel" value={form.mobile} onChange={set("mobile")} error={errors.mobile} />
          <Field id="password" label="Initial password" type="password" autoComplete="new-password"
                 value={form.password} onChange={set("password")} error={errors.password} />
          <div>
            <label htmlFor="departmentId" className="block text-sm font-medium text-slate-700">Department</label>
            <select id="departmentId" value={form.departmentId} onChange={set("departmentId")}
                    className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm">
              <option value="">Choose...</option>
              {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
            {errors.departmentId && <p className="mt-1 text-xs text-red-600">{errors.departmentId}</p>}
          </div>
          <Field id="specialization" label="Specialization" value={form.specialization} onChange={set("specialization")} error={errors.specialization} />
          <Field id="qualification" label="Qualification (optional)" value={form.qualification} onChange={set("qualification")} />
          <Field id="experienceYears" label="Years of experience (optional)" type="number" min="0" max="70"
                 value={form.experienceYears} onChange={set("experienceYears")} />
          <Field id="consultationDurationMin" label="Appointment length (minutes)" type="number" min="5" max="240"
                 value={form.consultationDurationMin} onChange={set("consultationDurationMin")} error={errors.consultationDurationMin} />
        </div>
        <button type="submit" disabled={saving}
                className="mt-6 rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
          {saving ? "Creating..." : "Create doctor"}
        </button>
      </form>
    </section>
  );
}