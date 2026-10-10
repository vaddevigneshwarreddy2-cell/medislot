import api from "./axios";

const data = (r) => r.data.data;

export const holdSlot = (doctorId, date, time) =>
  api.post("/appointments/hold", { doctorId: Number(doctorId), date, time }).then(data);

export const confirmAppointment = (appointmentId, reason) =>
  api.post("/appointments", { appointmentId, reason: reason || undefined }).then(data);

export const getAppointment = (id, signal) => api.get(`/appointments/${id}`, { signal }).then(data);

export const myAppointments = (scope, signal) =>
  api.get("/appointments/my", { params: { scope }, signal }).then(data);

export const cancelAppointment = (id, reason) =>
  api.put(`/appointments/${id}/cancel`, reason ? { reason } : {}).then(data);

export const rescheduleAppointment = (id, date, time) =>
  api.put(`/appointments/${id}/reschedule`, { date, time }).then(data);