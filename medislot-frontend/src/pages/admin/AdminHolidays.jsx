import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/axios";
import { getApiError } from "../../api/errors";
import { formatLong, todayString } from "../../utils/dates";

export default function AdminHolidays() {
  const [items, setItems] = useState([]);
  const [date, setDate] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    try {
      const r = await api.get("/admin/holidays");
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
    if (!date) { setError("Choose a date."); return; }
    if (!description.trim()) { setError("Enter a description."); return; }
    try {
      await api.post("/admin/holidays", { holidayDate: date, description: description.trim() });
      setDate(""); setDescription("");
      await load();
    } catch (err) {
      setError(getApiError(err).message);
    }
  }

  async function remove(id) {
    if (!window.confirm("Remove this holiday?")) return;
    try {
      await api.delete(`/admin/holidays/${id}`);
      setItems((xs) => xs.filter((x) => x.id !== id));
    } catch (err) {
      setError(getApiError(err).message);
    }
  }

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;
  const inputCls = "block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500";

  return (
    <section className="mx-auto max-w-3xl px-4 py-10">
      <Link to="/admin" className="text-sm font-medium text-brand-600 hover:underline">&larr; Dashboard</Link>
      <h1 className="mt-2 text-3xl font-bold text-slate-900">Hospital holidays</h1>
      <p className="text-slate-600">No slots are offered on these days for any doctor of your hospital.</p>
      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      <form onSubmit={add} className="mt-6 grid gap-3 rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:grid-cols-[auto_1fr_auto]">
        <div><label htmlFor="hol-date" className="sr-only">Date</label>
          <input id="hol-date" type="date" min={todayString()} className={inputCls} value={date} onChange={(e) => setDate(e.target.value)} /></div>
        <div><label htmlFor="hol-desc" className="sr-only">Description</label>
          <input id="hol-desc" className={inputCls} placeholder="Description (e.g. Festival)" maxLength={255} value={description} onChange={(e) => setDescription(e.target.value)} /></div>
        <button type="submit" className="rounded-lg bg-brand-600 px-5 py-2 text-sm font-medium text-white hover:bg-brand-700">Add</button>
      </form>

      <ul className="mt-6 divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white shadow-sm">
        {items.length === 0 && <li className="px-4 py-6 text-center text-slate-500">No upcoming holidays.</li>}
        {items.map((h) => (
          <li key={h.id} className="flex items-center justify-between px-4 py-3 text-sm">
            <span><span className="font-medium text-slate-800">{formatLong(h.holidayDate)}</span> - {h.description}</span>
            <button type="button" onClick={() => remove(h.id)} className="font-medium text-red-600 hover:underline">Remove</button>
          </li>
        ))}
      </ul>
    </section>
  );
}