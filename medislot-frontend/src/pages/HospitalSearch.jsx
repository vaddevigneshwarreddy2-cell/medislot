import { useEffect, useState } from "react";
import { isCancel } from "axios";
import { FALLBACK_CONFIG, getSearchConfig, searchHospitals } from "../api/hospitals";
import { getApiError } from "../api/errors";
import LocationPicker from "../components/LocationPicker";
import RadiusSelector from "../components/RadiusSelector";
import HospitalCard from "../components/HospitalCard";

export default function HospitalSearch() {
  const [config, setConfig] = useState(FALLBACK_CONFIG);
  const [radius, setRadius] = useState(null);
  const [origin, setOrigin] = useState(null);      // { lat, lng, label } - memory only, never stored
  const [nameInput, setNameInput] = useState("");
  const [name, setName] = useState("");            // applied on submit
  const [status, setStatus] = useState("idle");    // idle | loading | done | error
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  // Radius options and default come from the server configuration.
  useEffect(() => {
    let cancelled = false;
    getSearchConfig()
      .then((c) => { if (!cancelled) { setConfig(c); setRadius(c.defaultRadiusKm); } })
      .catch(() => { if (!cancelled) setRadius(FALLBACK_CONFIG.defaultRadiusKm); });
    return () => { cancelled = true; };
  }, []);

  // Search again whenever the location, radius or name changes. Stale responses are aborted.
  useEffect(() => {
    if (radius == null) return;
    if (!origin && !name) {
      setResult(null);
      setStatus("idle");
      return;
    }
    const controller = new AbortController();
    setStatus("loading");
    setError(null);
    searchHospitals({ name, origin, radiusKm: radius }, controller.signal)
      .then((data) => { setResult(data); setStatus("done"); })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setStatus("error");
      });
    return () => controller.abort();
  }, [origin, name, radius]);

  const largerOptions = config.radiusOptionsKm.filter((km) => radius != null && km > radius).slice(0, 3);

  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-3xl font-bold text-slate-900">Find a hospital</h1>
      <p className="mt-1 text-slate-600">Search near you, or look a hospital up by name.</p>

      <div className="mt-6 space-y-6 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Your location</h2>
          <LocationPicker origin={origin} onChange={setOrigin} />
        </div>

        <form
          onSubmit={(e) => { e.preventDefault(); setName(nameInput.trim()); }}
          role="search"
          aria-label="Search hospitals by name"
        >
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Hospital name</h2>
          <div className="flex gap-2">
            <label htmlFor="hospital-name" className="sr-only">Search hospitals by name</label>
            <input
              id="hospital-name"
              value={nameInput}
              onChange={(e) => setNameInput(e.target.value)}
              placeholder="Search hospitals..."
              maxLength={100}
              className="block flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <button type="submit" className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium hover:bg-slate-100">
              Search
            </button>
            {name && (
              <button
                type="button"
                onClick={() => { setNameInput(""); setName(""); }}
                className="rounded-lg px-3 py-2 text-sm font-medium text-brand-600 hover:underline"
              >
                Clear
              </button>
            )}
          </div>
        </form>

        {origin && radius != null && (
          <div>
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Search within</h2>
            <RadiusSelector options={config.radiusOptionsKm} value={radius} onChange={setRadius} />
          </div>
        )}
      </div>

      <div className="mt-8" aria-live="polite">
        {status === "idle" && (
          <p className="rounded-xl border border-dashed border-slate-300 p-8 text-center text-slate-500">
            Choose a location or enter a hospital name to see results.
          </p>
        )}

        {status === "loading" && <p className="p-8 text-center text-slate-500">Searching...</p>}

        {status === "error" && (
          <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-red-700">{error}</p>
        )}

        {status === "done" && result && result.count > 0 && (
          <>
            <h2 className="mb-4 text-lg font-semibold text-slate-800">
              {result.count} hospital{result.count === 1 ? "" : "s"} found
              {origin ? ` within ${result.radiusKm} km` : ""}
            </h2>
            <div className="grid gap-4 sm:grid-cols-2">
              {result.hospitals.map((h) => (
                <HospitalCard key={h.id} hospital={h} origin={origin} />
              ))}
            </div>
          </>
        )}

        {status === "done" && result && result.count === 0 && (
          <div className="rounded-xl border border-slate-200 bg-white p-8 text-center">
            {origin ? (
              <>
                <p className="text-lg font-semibold text-slate-800">No hospitals found within {radius} km.</p>
                {largerOptions.length > 0 ? (
                  <>
                    <p className="mt-1 text-slate-600">Try increasing the search distance.</p>
                    <div className="mt-4 flex flex-wrap justify-center gap-2">
                      {largerOptions.map((km) => (
                        <button
                          key={km}
                          type="button"
                          onClick={() => setRadius(km)}
                          className="rounded-full border border-brand-600 px-4 py-1.5 text-sm font-medium text-brand-600 hover:bg-brand-50"
                        >
                          {km} km
                        </button>
                      ))}
                    </div>
                  </>
                ) : (
                  <p className="mt-1 text-slate-600">Try a different location, or search by hospital name.</p>
                )}
              </>
            ) : (
              <p className="text-lg font-semibold text-slate-800">No hospitals match &quot;{name}&quot;.</p>
            )}
          </div>
        )}
      </div>
    </section>
  );
}