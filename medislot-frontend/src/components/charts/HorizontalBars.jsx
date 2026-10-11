// rows: [{ name, booked, completed, cancelled, noShow }]
export default function HorizontalBars({ title, rows }) {
  const max = Math.max(1, ...rows.map((r) => r.booked));
  return (
    <div className="mt-8">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">{title}</h2>
      {rows.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">No appointments in this period.</p>
      ) : (
        <ul className="mt-2 space-y-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          {rows.map((r) => (
            <li key={r.name}>
              <div className="flex flex-wrap items-baseline justify-between gap-2 text-sm">
                <span className="font-medium text-slate-800">{r.name}</span>
                <span className="text-xs text-slate-500">
                  {r.booked} booked &middot; {r.completed} completed &middot; {r.cancelled} cancelled &middot; {r.noShow} no-show
                </span>
              </div>
              <div className="mt-1 h-2 w-full rounded bg-slate-100" aria-hidden="true">
                <div className="h-2 rounded bg-brand-500" style={{ width: `${Math.round((r.booked / max) * 100)}%` }} />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}