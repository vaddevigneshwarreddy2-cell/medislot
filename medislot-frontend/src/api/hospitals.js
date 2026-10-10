import api from "./axios";

export const FALLBACK_CONFIG = { defaultRadiusKm: 3, radiusOptionsKm: [1, 3, 5, 10, 25, 50] };

export function getSearchConfig() {
  return api.get("/hospitals/config").then((r) => r.data.data);
}

// name and/or origin ({lat, lng}). The server does the distance filtering and sorting.
export function searchHospitals({ name, origin, radiusKm }, signal) {
  const params = {};
  if (name) params.name = name;
  if (origin) {
    params.latitude = origin.lat;
    params.longitude = origin.lng;
    params.radiusKm = radiusKm;
  }
  const path = origin && !name ? "/hospitals/nearby" : "/hospitals/search";
  return api.get(path, { params, signal }).then((r) => r.data.data);
}

export function getHospital(id, origin, signal) {
  const params = origin ? { latitude: origin.lat, longitude: origin.lng } : {};
  return api.get(`/hospitals/${id}`, { params, signal }).then((r) => r.data.data);
}