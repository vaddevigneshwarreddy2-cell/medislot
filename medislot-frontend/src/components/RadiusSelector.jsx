export default function RadiusSelector({ options, value, onChange }) {
  return (
    <div role="group" aria-label="Search radius" className="flex flex-wrap gap-2">
      {options.map((km) => (
        <button
          key={km}
          type="button"
          aria-pressed={km === value}
          onClick={() => onChange(km)}
          className={`rounded-full border px-4 py-1.5 text-sm font-medium ${
            km === value
              ? "border-brand-600 bg-brand-600 text-white"
              : "border-slate-300 bg-white text-slate-700 hover:bg-slate-100"
          }`}
        >
          {km} km
        </button>
      ))}
    </div>
  );
}