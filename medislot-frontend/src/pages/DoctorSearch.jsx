import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { isCancel } from "axios";
import { searchDoctors } from "../api/doctors";
import { FALLBACK_CONFIG, getSearchConfig } from "../api/hospitals";
import { getApiError } from "../api/errors";
import LocationPicker from "../components/LocationPicker";
import RadiusSelector from "../components/RadiusSelector";
import DoctorCard from "../components/DoctorCard";

export default function DoctorSearch() {
  const [params, setParams] = useSearchParams();
  const hospitalId = params.get("hospitalId");

  const [config, setConfig] = useState(FALLBACK_CONFIG);
  const [radius, setRadius] = useState(FALLBACK_CONFIG.defaultRadiusKm);
  const [origin, setOrigin] = useState(null); // memory only, never stored
  const [nameInput, setNameInput] = useState("");
  const [specInput, setSpecInput] = useState("");
  const [filters, setFilters] = useState({ name: "", specialization: "" });
  const [status, setStatus] = useState("loading");
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getSearchConfig()
      .then((c) => { setConfig(c); setRadius(c.defaultRadiusKm); })
      .catch(() => {});
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setStatus("loading");
    setError(null);
    searchDoctors({ ...filters, hospitalId, origin, radiusKm: radius }, controller.signal)
      .then((data) => { setResult(data); setStatus("done"); })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setStatus("error");
      });
    return () => controller.abort();
  }, [filters, hospitalId, origin, radius]);

  const largerOptions = config.radiusOptionsKm.filter((km) => km > radius).slice(0, 3);

  function submit(e) {
    e.preventDefault();
    setFilters({ name: nameInput.trim(), specialization: specInput.trim() });
  }

  function clearAll() {
    setNameInput(""); setSpecInput("");
    setFilters({ name: "", specialization: "" });
    setOrigin(null);
    setParams({});
  }

  const inputCls = "block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500";

  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-3xl font-bold text-slate-900">Find a doctor</h1>
      <p className="mt-1 text-slate-600">Search by name or specialization, or near a location.</p>

      <div className="mt-6 space-y-6 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <form onSubmit={submit} className="grid gap-3 sm:grid-cols-[1fr_1fr_auto]" role="search" aria-label="Search doctors">
          <div>
            <label htmlFor="doc-name" className="sr-only">Doctor name</label>
            <input id="doc-name" className={inputCls} placeholder="Doctor name" maxLength={100}
                   value={nameInput} onChange={(e) => setNameInput(e.target.value)} />
          </div>
          <div>
            <label htmlFor="doc-spec" className="sr-only">Specialization</label>
            <input id="doc-spec" className={inputCls} placeholder="Specialization (e.g. cardio)" maxLength={100}
                   value={specInput} onChange={(e) => setSpecInput(e.target.value)} />
          </div>
          <button type="submit" className="rounded-lg bg-brand-600 px-5 py-2 text-sm font-medium text-white hover:bg-brand-700">
            Search
          </button>
        </form>

        {hospitalId && (
          <p className="rounded-lg bg-brand-50 px-3 py-2 text-sm">
            Showing doctors of one hospital.{" "}
            <button type="button" onClick={() => setParams({})} className="font-medium text-brand-600 hover:underline">
              Show all hospitals
            </button>
          </p>
        )}

        <div>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Near a location (optional)</h2>
          <LocationPicker origin={origin} onChange={setOrigin} />
        </div>

        {origin && (
          <div>
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Search within</h2>
            <RadiusSelector options={config.radiusOptionsKm} value={radius} onChange={setRadius} />
          </div>
        )}

        {(filters.name || filters.specialization || origin || hospitalId) && (
          <button type="button" onClick={clearAll} className="text-sm font-medium text-brand-600 hover:underline">
            Clear all filters
          </button>
        )}
      </div>

      <div className="mt-8" aria-live="polite">
        {status === "loading" && <p className="p-8 text-center text-slate-500">Searching...</p>}
        {status === "error" && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-red-700">{error}</p>}

        {status === "done" && result && result.count > 0 && (
          <>
            <h2 className="mb-4 text-lg font-semibold text-slate-800">
              {result.count} doctor{result.count === 1 ? "" : "s"} found
            </h2>
            <div className="grid gap-4 sm:grid-cols-2">
              {result.doctors.map((d) => <DoctorCard key={d.id} doctor={d} />)}
            </div>
          </>
        )}

        {status === "done" && result && result.count === 0 && (
          <div className="rounded-xl border border-slate-200 bg-white p-8 text-center">
            <p className="text-lg font-semibold text-slate-800">
              {origin ? `No matching doctors within ${radius} km.` : "No doctors match your search."}
            </p>
            {origin && largerOptions.length > 0 ? (
              <div className="mt-4 flex flex-wrap justify-center gap-2">
                {largerOptions.map((km) => (
                  <button key={km} type="button" onClick={() => setRadius(km)}
                          className="rounded-full border border-brand-600 px-4 py-1.5 text-sm font-medium text-brand-600 hover:bg-brand-50">
                    {km} km
                  </button>
                ))}
              </div>
            ) : (
              <p className="mt-1 text-slate-600">
                Try different words, or <Link to="/hospitals" className="text-brand-600 hover:underline">browse hospitals</Link>.
              </p>
            )}
          </div>
        )}
      </div>
    </section>
  );
}