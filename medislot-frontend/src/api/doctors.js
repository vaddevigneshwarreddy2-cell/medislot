import api from "./axios";

export function searchDoctors({ name, specialization, hospitalId, origin, radiusKm }, signal) {
  const params = {};
  if (name) params.name = name;
  if (specialization) params.specialization = specialization;
  if (hospitalId) params.hospitalId = hospitalId;
  if (origin) {
    params.latitude = origin.lat;
    params.longitude = origin.lng;
    params.radiusKm = radiusKm;
  }
  return api.get("/doctors", { params, signal }).then((r) => r.data.data);
}

export const getDoctor = (id, signal) => api.get(`/doctors/${id}`, { signal }).then((r) => r.data.data);

export const getUpcoming = (id, days, signal) =>
  api.get(`/doctors/${id}/availability/upcoming`, { params: { days }, signal }).then((r) => r.data.data);

export const getAvailability = (id, date, signal) =>
  api.get(`/doctors/${id}/availability`, { params: { date }, signal }).then((r) => r.data.data);