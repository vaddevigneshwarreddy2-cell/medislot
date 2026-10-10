import { useRef, useState } from "react";
import { getCurrentLocation } from "../utils/geolocation";
import { geocode } from "../api/geocode";

const QUICK_AREAS = ["Banjara Hills", "Madhapur", "Kukatpally", "Gachibowli", "Hyderabad"];

function shortLabel(label) {
  return label.split(",").slice(0, 3).join(",").trim();
}

export default function LocationPicker({ origin, onChange }) {
  const [query, setQuery] = useState("");
  const [busy, setBusy] = useState(null); // "gps" | "search" | null
  const [error, setError] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const controller = useRef(null);

  function choose(place) {
    setSuggestions([]);
    setQuery("");
    setError(null);
    onChange({ lat: place.lat, lng: place.lng, label: shortLabel(place.label) });
  }

  async function useMyLocation() {
    setError(null);
    setSuggestions([]);
    setBusy("gps");
    try {
      const p = await getCurrentLocation();
      onChange({ lat: p.lat, lng: p.lng, label: "Your current location" });
    } catch (e) {
      setError(e.message || "Unable to determine your location.");
    } finally {
      setBusy(null);
    }
  }

  async function runSearch(text) {
    const q = text.trim();
    if (q.length < 2) {
      setError("Enter at least 2 characters.");
      return;
    }
    controller.current?.abort();
    const c = new AbortController();
    controller.current = c;
    setError(null);
    setSuggestions([]);
    setBusy("search");
    try {
      const rows = await geocode(q, c.signal);
      if (rows.length === 0) setError(`No places found for "${q}". Try a nearby area or a city name.`);
      else if (rows.length === 1) choose(rows[0]);
      else setSuggestions(rows);
    } catch (e) {
      if (e.name === "AbortError") return;
      setError("Could not look up that location right now. Try again, or use your current location.");
    } finally {
      if (controller.current === c) setBusy(null);
    }
  }

  return (
    <div className="space-y-4">
      {origin && (
        <div className="flex items-center justify-between gap-3 rounded-lg bg-brand-50 px-3 py-2 text-sm">
          <span>
            <span aria-hidden="true">&#128205;</span> Searching near{" "}
            <span className="font-semibold text-slate-800">{origin.label}</span>
          </span>
          <button type="button" onClick={() => onChange(null)} className="font-medium text-brand-600 hover:underline">
            Clear
          </button>
        </div>
      )}

      <button
        type="button"
        onClick={useMyLocation}
        disabled={busy !== null}
        className="w-full rounded-lg bg-brand-600 px-4 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60 sm:w-auto"
      >
        {busy === "gps" ? "Finding your location..." : "Use My Current Location"}
      </button>

      <form
        onSubmit={(e) => { e.preventDefault(); runSearch(query); }}
        className="flex gap-2"
        role="search"
        aria-label="Search location"
      >
        <div className="flex-1">
          <label htmlFor="location-q" className="sr-only">Search location</label>
          <input
            id="location-q"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Enter area, city or location..."
            maxLength={100}
            className="block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>
        <button
          type="submit"
          disabled={busy !== null}
          className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium hover:bg-slate-100 disabled:opacity-60"
        >
          {busy === "search" ? "Searching..." : "Search"}
        </button>
      </form>

      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span className="text-slate-500">Try:</span>
        {QUICK_AREAS.map((a) => (
          <button
            key={a}
            type="button"
            disabled={busy !== null}
            onClick={() => { setQuery(a); runSearch(a); }}
            className="rounded-full bg-slate-100 px-3 py-1 text-slate-700 hover:bg-slate-200 disabled:opacity-60"
          >
            {a}
          </button>
        ))}
      </div>

      {error && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
      )}

      {suggestions.length > 0 && (
        <div>
          <p className="mb-1 text-sm font-medium text-slate-700">Which one did you mean?</p>
          <ul className="divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
            {suggestions.map((s, i) => (
              <li key={`${s.lat}-${s.lng}-${i}`}>
                <button
                  type="button"
                  onClick={() => choose(s)}
                  className="block w-full px-3 py-2 text-left text-sm hover:bg-slate-50"
                >
                  {s.label}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}