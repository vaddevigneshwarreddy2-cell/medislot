// Links are built ONLY from the hospital's stored coordinates. Never add patient,
// appointment or medical data to these URLs.
export function hasCoordinates(h) {
  return (
    h != null &&
    h.latitude != null && h.longitude != null &&
    Number.isFinite(Number(h.latitude)) && Number.isFinite(Number(h.longitude))
  );
}

function point(h) {
  return `${Number(h.latitude).toFixed(7)},${Number(h.longitude).toFixed(7)}`;
}

export function viewLocationUrl(h) {
  return `https://www.google.com/maps/search/?api=1&query=${point(h)}`;
}

export function directionsUrl(h, travelMode = "driving") {
  return `https://www.google.com/maps/dir/?api=1&destination=${point(h)}&travelmode=${travelMode}`;
}