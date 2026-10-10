import { NavLink } from "react-router-dom";

const ITEMS = [
  ["/reception", "Overview", true],
  ["/reception/check-in", "QR check-in"],
  ["/reception/queue", "Queue"],
  ["/reception/walk-in", "Walk-in"],
  ["/reception/appointments", "Appointments"],
];

export default function ReceptionPage({ title, children }) {
  return (
    <section className="mx-auto max-w-5xl px-4 py-10">
      <h1 className="text-3xl font-bold text-slate-900">{title}</h1>
      <nav aria-label="Reception sections" className="mt-4 flex flex-wrap gap-2">
        {ITEMS.map(([to, label, end]) => (
          <NavLink key={to} to={to} end={end}
                   className={({ isActive }) =>
                     `rounded-full border px-4 py-1.5 text-sm font-medium ${
                       isActive ? "border-brand-600 bg-brand-600 text-white" : "border-slate-300 bg-white hover:bg-slate-100"}`}>
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="mt-6">{children}</div>
    </section>
  );
}