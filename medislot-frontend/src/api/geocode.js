// Free OpenStreetMap Nominatim lookup. Uses plain fetch on purpose: the app's Axios
// instance would attach the user's login token to a third-party request.
// Only call this on an explicit search action (usage policy forbids per-keystroke autocomplete).
export async function geocode(query, signal) {
  const params = new URLSearchParams({
    format: "jsonv2",
    limit: "5",
    countrycodes: "in",
    "accept-language": "en",
    q: query,
  });
  const res = await fetch(`https://nominatim.openstreetmap.org/search?${params}`, {
    signal,
    headers: { Accept: "application/json" },
  });
  if (!res.ok) throw new Error("Geocoding failed");
  const rows = await res.json();
  return rows
    .map((r) => ({ lat: Number(r.lat), lng: Number(r.lon), label: r.display_name }))
    .filter((r) => Number.isFinite(r.lat) && Number.isFinite(r.lng));
}