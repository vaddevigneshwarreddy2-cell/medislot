import { useEffect, useState } from "react";
import api from "../api/axios";
import { getApiError } from "../api/errors";
import Field from "../components/Field";
import StaffManager from "../components/StaffManager";

const EMPTY = { name: "", phone: "", email: "", addressLine: "", city: "", state: "", postalCode: "", latitude: "", longitude: "" };

export default function SuperAdmin() {
  const [hospitals, setHospitals] = useState([]);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function load() {
    try { setHospitals((await api.get("/super-admin/hospitals")).data.data); setError(null); }
    catch (err) { setError(getApiError(err).message); }
    finally { setLoading(false); }
  }
  useEffect(() => { load(); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function create(ev) {
    ev.preventDefault();
    setMsg(null);
    const er = {};
    if (!form.name.trim()) er.name = "Enter the name.";
    if (!form.addressLine.trim()) er.addressLine = "Enter the address.";
    if (!form.city.trim()) er.city = "Enter the city.";
    if (!form.state.trim()) er.state = "Enter the state.";
    const lat = Number(form.latitude), lng = Number(form.longitude);
    if (form.latitude === "" || !Number.isFinite(lat) || lat < -90 || lat > 90) er.latitude = "Between -90 and 90.";
    if (form.longitude === "" || !Number.isFinite(lng) || lng < -180 || lng > 180) er.longitude = "Between -180 and 180.";
    setErrors(er);
    if (Object.keys(er).length) return;
    setSaving(true);
    try {
      await api.post("/super-admin/hospitals", {
        profile: { name: form.name.trim(), phone: form.phone.trim() || null, email: form.email.trim() || null },
        location: { addressLine: form.addressLine.trim(), city: form.city.trim(), state: form.state.trim(),
                    postalCode: form.postalCode.trim() || null, latitude: lat, longitude: lng } });
      setForm(EMPTY);
      setMsg("Hospital created. Now add its hospital admin.");
      await load();
    } catch (err) { setError(getApiError(err).message); } finally { setSaving(false); }
  }

  async function toggle(h) {
    setMsg(null);
    try { await api.patch(`/super-admin/hospitals/${h.id}/active`, { active: !h.active }); await load(); }
    catch (err) { setError(getApiError(err).message); }
  }

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;

  return (
    <section className="mx-auto max-w-5xl px-4 py-10">
      <h1 className="text-3xl font-bold text-slate-900">Platform administration</h1>
      <p className="text-slate-600">Hospitals and their administrators. Clinical data is not available here.</p>
      <div aria-live="polite">
        {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        {msg && <p role="status" className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}
      </div>

      <div className="mt-6 overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-3">Hospital</th><th className="px-4 py-3">City</th><th className="px-4 py-3">Status</th><th className="px-4 py-3" /></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {hospitals.map((h) => (
              <tr key={h.id} className={selected === h.id ? "bg-brand-50" : ""}>
                <td className="px-4 py-3 font-medium text-slate-800">{h.name}</td>
                <td className="px-4 py-3">{h.city}</td>
                <td className="px-4 py-3">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${h.active ? "bg-green-100 text-green-700" : "bg-slate-200 text-slate-600"}`}>
                    {h.active ? "Active" : "Disabled"}
                  </span>
                </td>
                <td className="space-x-3 px-4 py-3 text-right whitespace-nowrap">
                  <button type="button" onClick={() => setSelected(selected === h.id ? null : h.id)} className="font-medium text-brand-600 hover:underline">
                    {selected === h.id ? "Hide admins" : "Admins"}
                  </button>
                  <button type="button" onClick={() => toggle(h)} className="font-medium text-slate-600 hover:underline">{h.active ? "Disable" : "Enable"}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selected && (
        <div className="mt-6">
          <h2 className="text-lg font-semibold text-slate-900">Hospital administrators</h2>
          <StaffManager basePath={`/super-admin/hospitals/${selected}/admins`} noun="Hospital admin" />
        </div>
      )}

      <form onSubmit={create} noValidate className="mt-8 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Add a hospital</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2"><Field id="n-name" label="Hospital name" value={form.name} onChange={set("name")} error={errors.name} /></div>
          <Field id="n-phone" label="Phone (optional)" value={form.phone} onChange={set("phone")} />
          <Field id="n-email" label="Email (optional)" value={form.email} onChange={set("email")} />
          <div className="sm:col-span-2"><Field id="n-addr" label="Address" value={form.addressLine} onChange={set("addressLine")} error={errors.addressLine} /></div>
          <Field id="n-city" label="City" value={form.city} onChange={set("city")} error={errors.city} />
          <Field id="n-state" label="State" value={form.state} onChange={set("state")} error={errors.state} />
          <Field id="n-pin" label="Postal code (optional)" value={form.postalCode} onChange={set("postalCode")} />
          <span />
          <Field id="n-lat" label="Latitude" value={form.latitude} onChange={set("latitude")} error={errors.latitude} hint="Example: 17.4126000" />
          <Field id="n-lng" label="Longitude" value={form.longitude} onChange={set("longitude")} error={errors.longitude} hint="Example: 78.4482000" />
        </div>
        <button type="submit" disabled={saving} className="mt-6 rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
          {saving ? "Creating..." : "Create hospital"}
        </button>
      </form>
    </section>
  );
}