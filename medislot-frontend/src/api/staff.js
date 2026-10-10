import api from "./axios";

const d = (r) => r.data.data;

// patient
export const getQr = (id) => api.get(`/appointments/${id}/qr`).then(d);
export const getQueueStatus = (id) => api.get(`/appointments/${id}/queue`).then(d);

// reception
export const recDoctors = () => api.get("/reception/doctors").then(d);
export const recAppointments = (params) => api.get("/reception/appointments", { params }).then(d);
export const recQueue = (doctorId) =>
  api.get("/reception/queue", { params: doctorId ? { doctorId } : {} }).then(d);
export const checkInByCode = (code) => api.post("/reception/check-in", { code }).then(d);
export const checkInById = (id) => api.post(`/reception/appointments/${id}/check-in`).then(d);
export const registerWalkIn = (body) => api.post("/reception/walk-ins", body).then(d);
export const changeStatus = (id, status, note) =>
  api.put(`/reception/appointments/${id}/status`, { status, note: note || undefined }).then(d);

// doctor
export const doctorQueue = () => api.get("/doctor/queue").then(d);
export const callNext = () => api.post("/doctor/queue/call-next").then(d);
export const callPatient = (id) => api.post(`/doctor/queue/${id}/call`).then(d);
export const startConsultation = (id) => api.post(`/doctor/queue/${id}/start`).then(d);
export const completeConsultation = (id) => api.post(`/doctor/queue/${id}/complete`).then(d);