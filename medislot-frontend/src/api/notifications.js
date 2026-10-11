import api from "./axios";

const d = (r) => r.data.data;

export const listNotifications = (unreadOnly) =>
  api.get("/notifications", { params: { unreadOnly } }).then(d);
export const unreadCount = () => api.get("/notifications/unread-count").then((r) => r.data.data.count);
export const markRead = (id) => api.put(`/notifications/${id}/read`).then(d);
export const markAllRead = () => api.put("/notifications/read-all").then(d);
export const confirmAttendance = (appointmentId) =>
  api.put(`/appointments/${appointmentId}/attendance`).then(d);