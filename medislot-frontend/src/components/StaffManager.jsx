import { useEffect, useState } from "react";
import api from "../api/axios";
import { getApiError } from "../api/errors";
import Field from "./Field";

const EMPTY = { fullName: "", email: "", mobile: "", password: "" };

export default function StaffManager({ basePath, noun }) {
  const [members, setMembers] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function load() {
    try {
      setMembers((await api.get(basePath)).data.data);
      setError(null);
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => { setLoading(true); setMsg(null); load(); /* eslint-disable-next-line */ }, [basePath]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  function validate() {
    const e = {};
    if (!form.fullName.trim()) e.fullName = "Enter the name.";
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) e.email = "Enter a valid email.";
    if (form.mobile && !/^\+?[0-9]{10,15}$/.test(form.mobile.trim())) e.mobile = "Use 10-15 digits.";
    if (form.password.length < 8 || !/[a-z]/.test(form.password) || !/[A-Z]/.test(form.password) || !/\d/.test(form.password))
      e.password = "At least 8 characters with upper case, lower case and a digit.";
    return e;
  }

  async function submit(ev) {
    ev.preventDefault();
    setMsg(null);
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length) return;
    setSaving(true);
    try {
      const body = { fullName: form.fullName.trim(), email: form.email.trim(), password: form.password };
      if (form.mobile.trim()) body.mobile = form.mobile.trim();
      await api.post(basePath, body);
      setForm(EMPTY);
      setMsg(`${noun} created. Share the initial password with them securely.`);
      setError(null);
      await load();
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setSaving(false);
    }
  }

  async function toggle(m) {
    setMsg(null);
    try {
      await api.patch(`${basePath}/${m.id}/active`, { active: !m.active });
      await load();
    } catch (err) {
      setError(getApiError(err).message);
    }
  }

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;

  return (
    <div>
      <div aria-live="polite">
        {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        {msg && <p role="status" className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}
      </div>

      <div className="mt-4 overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-3">Name</th><th className="px-4 py-3">Mobile</th><th className="px-4 py-3">Status</th><th className="px-4 py-3" /></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {members.length === 0 && <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-500">None yet.</td></tr>}
            {members.map((m) => (
              <tr key={m.id}>
                <td className="px-4 py-3"><div className="font-medium text-slate-800">{m.fullName}</div><div className="text-xs text-slate-500">{m.email}</div></td>
                <td className="px-4 py-3">{m.mobile || "-"}</td>
                <td className="px-4 py-3">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${m.active ? "bg-green-100 text-green-700" : "bg-slate-200 text-slate-600"}`}>
                    {m.active ? "Active" : "Disabled"}
                  </span>
                </td>
                <td className="px-4 py-3 text-right">
                  <button type="button" onClick={() => toggle(m)} className="font-medium text-slate-600 hover:underline">
                    {m.active ? "Disable" : "Enable"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form onSubmit={submit} noValidate className="mt-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Add a {noun.toLowerCase()}</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <Field id="s-name" label="Full name" value={form.fullName} onChange={set("fullName")} error={errors.fullName} />
          <Field id="s-email" label="Email (used to log in)" type="email" value={form.email} onChange={set("email")} error={errors.email} />
          <Field id="s-mobile" label="Mobile (optional)" type="tel" value={form.mobile} onChange={set("mobile")} error={errors.mobile} />
          <Field id="s-pass" label="Initial password" type="password" autoComplete="new-password"
                 value={form.password} onChange={set("password")} error={errors.password} />
        </div>
        <button type="submit" disabled={saving}
                className="mt-6 rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
          {saving ? "Creating..." : `Create ${noun.toLowerCase()}`}
        </button>
      </form>
    </div>
  );
}