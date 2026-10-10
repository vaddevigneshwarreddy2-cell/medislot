import api from "./axios";

const d = (r) => r.data.data;

// doctor: consultation
export const getConsultation = (id) => api.get(`/doctor/consultations/${id}`).then(d);
export const saveConsultation = (id, body) => api.put(`/doctor/consultations/${id}`, body).then(d);
export const completeConsultation = (id, body) => api.post(`/doctor/consultations/${id}/complete`, body).then(d);

// prescriptions (role decides which side of the API is used)
const base = (role) => (role === "DOCTOR" ? "/doctor/prescriptions" : "/patient/prescriptions");
export const listPrescriptions = (role) => api.get(base(role)).then(d);
export const getPrescription = (role, id) => api.get(`${base(role)}/${id}`).then(d);
export const getFollowUps = () => api.get("/patient/follow-ups").then(d);

// The PDF endpoint needs the login token, so it is fetched with Axios and saved from memory.
export async function downloadPdf(role, id) {
  const res = await api.get(`${base(role)}/${id}/pdf`, { responseType: "blob" });
  const url = URL.createObjectURL(res.data);
  const a = document.createElement("a");
  a.href = url;
  a.download = `prescription-${id}.pdf`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}