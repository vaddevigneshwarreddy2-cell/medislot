import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/axios";
import { getApiError } from "../../api/errors";

export default function AdminDepartments() {
  const [items, setItems] = useState([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    try {
      const r = await api.get("/admin/departments");
      setItems(r.data.data);
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => { load(); }, []);

  async function add(e) {
    e.preventDefault();
    setError(null);
    if (!name.trim()) { setError("Enter a department name."); return; }
    try {
      await api.post("/admin/departments", { name: name.trim(), description: description.trim() });
      setName(""); setDescription("");
      await load();
    } catch (err) {
      setError(getApiError(err).message);
    }
  }

  async function toggle(d) {
    setError(null);
    try {
      await api.patch(`/admin/departments/${d.id}/active`, { active: !d.active });
      await load();
    } catch (err) {
      setError(getApiError(err).message);
    }
  }

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;
  const inputCls = "block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500";

  return (
    <section className="mx-auto max-w-3xl px-4 py-10">
      <Link to="/admin" className="text-sm font-medium text-brand-600 hover:underline">&larr; Dashboard</Link>
      <h1 className="mt-2 text-3xl font-bold text-slate-900">Departments</h1>
      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      <form onSubmit={add} className="mt-6 grid gap-3 rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:grid-cols-[1fr_1fr_auto]">
        <div><label htmlFor="dep-name" className="sr-only">Name</label>
          <input id="dep-name" className={inputCls} placeholder="Department name" maxLength={100} value={name} onChange={(e) => setName(e.target.value)} /></div>
        <div><label htmlFor="dep-desc" className="sr-only">Description</label>
          <input id="dep-desc" className={inputCls} placeholder="Description (optional)" maxLength={500} value={description} onChange={(e) => setDescription(e.target.value)} /></div>
        <button type="submit" className="rounded-lg bg-brand-600 px-5 py-2 text-sm font-medium text-white hover:bg-brand-700">Add</button>
      </form>

      <ul className="mt-6 divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white shadow-sm">
        {items.length === 0 && <li className="px-4 py-6 text-center text-slate-500">No departments yet.</li>}
        {items.map((d) => (
          <li key={d.id} className="flex items-center justify-between px-4 py-3">
            <div>
              <p className={`font-medium ${d.active ? "text-slate-800" : "text-slate-400"}`}>{d.name}</p>
              {d.description && <p className="text-sm text-slate-500">{d.description}</p>}
            </div>
            <button type="button" onClick={() => toggle(d)} className="text-sm font-medium text-slate-600 hover:underline">
              {d.active ? "Disable" : "Enable"}
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}