import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listNotifications, markAllRead, markRead } from "../api/notifications";
import { getApiError } from "../api/errors";

export default function Notifications() {
  const navigate = useNavigate();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [items, setItems] = useState(null);
  const [error, setError] = useState(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    setItems(null);
    listNotifications(unreadOnly).then(setItems).catch((e) => setError(getApiError(e).message));
  }, [unreadOnly, reload]);

  async function open(n) {
    try { if (!n.read) await markRead(n.id); } catch { /* opening the page still works */ }
    if (n.link) navigate(n.link); else setReload((x) => x + 1);
  }

  async function readAll() {
    try { await markAllRead(); setReload((x) => x + 1); } catch (e) { setError(getApiError(e).message); }
  }

  const tab = (value, label) => (
    <button type="button" onClick={() => setUnreadOnly(value)} aria-pressed={unreadOnly === value}
            className={`rounded-full border px-4 py-1.5 text-sm font-medium ${
              unreadOnly === value ? "border-brand-600 bg-brand-600 text-white" : "border-slate-300 bg-white hover:bg-slate-100"}`}>
      {label}
    </button>
  );

  return (
    <section className="mx-auto max-w-3xl px-4 py-10">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-3xl font-bold text-slate-900">Notifications</h1>
        <button type="button" onClick={readAll} className="text-sm font-medium text-brand-600 hover:underline">
          Mark all read
        </button>
      </div>
      <div className="mt-4 flex gap-2" role="group" aria-label="Filter">
        {tab(false, "All")}{tab(true, "Unread")}
      </div>

      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      {!items && !error && <p className="p-8 text-center text-slate-500">Loading...</p>}
      {items && items.length === 0 && (
        <p className="mt-6 rounded-xl border border-dashed border-slate-300 p-10 text-center text-slate-600">
          {unreadOnly ? "No unread notifications." : "You have no notifications yet."}
        </p>
      )}

      <ul className="mt-6 space-y-3">
        {(items || []).map((n) => (
          <li key={n.id}>
            <button type="button" onClick={() => open(n)}
                    className={`block w-full rounded-xl border p-4 text-left shadow-sm hover:border-brand-500 ${
                      n.read ? "border-slate-200 bg-white" : "border-brand-200 bg-brand-50"}`}>
              <div className="flex items-start justify-between gap-3">
                <p className={`font-semibold ${n.read ? "text-slate-700" : "text-slate-900"}`}>
                  {!n.read && <span className="mr-2 inline-block h-2 w-2 rounded-full bg-brand-600" aria-label="Unread" />}
                  {n.title}
                </p>
                <time className="shrink-0 text-xs text-slate-500" dateTime={n.createdAt}>
                  {new Date(n.createdAt).toLocaleString()}
                </time>
              </div>
              <p className="mt-1 text-sm text-slate-600">{n.message}</p>
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}