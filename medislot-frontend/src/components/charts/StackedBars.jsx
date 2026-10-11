export const COLORS = { completed: "#16a34a", other: "#0e7fd9", noShow: "#d97706", cancelled: "#dc2626" };

const LEGEND = [
  ["completed", "Completed"],
  ["other", "Booked / in progress"],
  ["noShow", "No-show"],
  ["cancelled", "Cancelled"],
];

function shortLabel(label, unit) {
  return unit === "month" ? label.slice(0, 7) : label.slice(5);
}

// data: [{ label, booked, completed, cancelled, noShow }]
export default function StackedBars({ data, unit = "day" }) {
  const W = 640, H = 240, L = 36, R = 8, T = 10, B = 28;
  const plotW = W - L - R;
  const plotH = H - T - B;
  const max = Math.max(1, ...data.map((d) => d.booked));
  const n = data.length;
  const slot = n ? plotW / n : plotW;
  const bw = Math.max(2, slot * 0.7);
  const step = Math.max(1, Math.ceil(n / 8));
  const total = data.reduce((s, d) => s + d.booked, 0);
  const scale = plotH / max;

  return (
    <div>
      <svg viewBox={`0 0 ${W} ${H}`} className="w-full" role="img"
           aria-label={`Appointments per ${unit}: ${total} in total over ${n} periods`}>
        {[0, 0.5, 1].map((f) => {
          const y = T + plotH - f * plotH;
          return (
            <g key={f}>
              <line x1={L} x2={W - R} y1={y} y2={y} stroke="#e2e8f0" />
              <text x={L - 4} y={y + 4} textAnchor="end" fontSize="10" fill="#64748b">{Math.round(f * max)}</text>
            </g>
          );
        })}
        {data.map((d, i) => {
          const other = Math.max(0, d.booked - d.completed - d.cancelled - d.noShow);
          const segments = [["completed", d.completed], ["other", other], ["noShow", d.noShow], ["cancelled", d.cancelled]];
          const x = L + i * slot + (slot - bw) / 2;
          let acc = 0;
          return (
            <g key={d.label}>
              <title>{`${d.label}: ${d.booked} booked, ${d.completed} completed, ${d.cancelled} cancelled, ${d.noShow} no-show`}</title>
              {segments.map(([key, value]) => {
                if (value <= 0) return null;
                const h = value * scale;
                acc += value;
                return <rect key={key} x={x} y={T + plotH - acc * scale} width={bw} height={h} fill={COLORS[key]} />;
              })}
              {i % step === 0 && (
                <text x={x + bw / 2} y={H - 10} textAnchor="middle" fontSize="10" fill="#64748b">
                  {shortLabel(d.label, unit)}
                </text>
              )}
            </g>
          );
        })}
      </svg>

      <ul className="mt-2 flex flex-wrap gap-4 text-xs text-slate-600">
        {LEGEND.map(([key, label]) => (
          <li key={key} className="flex items-center gap-1.5">
            <span className="inline-block h-3 w-3 rounded-sm" style={{ backgroundColor: COLORS[key] }} aria-hidden="true" />
            {label}
          </li>
        ))}
      </ul>

      <details className="mt-3">
        <summary className="cursor-pointer text-sm font-medium text-brand-600">Show data as a table</summary>
        <div className="mt-2 overflow-x-auto rounded-lg border border-slate-200">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 text-left text-slate-500">
              <tr><th className="px-3 py-2">Period</th><th className="px-3 py-2">Booked</th><th className="px-3 py-2">Completed</th>
                  <th className="px-3 py-2">Cancelled</th><th className="px-3 py-2">No-show</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.map((d) => (
                <tr key={d.label}><td className="px-3 py-2">{d.label}</td><td className="px-3 py-2">{d.booked}</td>
                  <td className="px-3 py-2">{d.completed}</td><td className="px-3 py-2">{d.cancelled}</td>
                  <td className="px-3 py-2">{d.noShow}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </div>
  );
}