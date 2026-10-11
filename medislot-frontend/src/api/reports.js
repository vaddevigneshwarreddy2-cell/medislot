import api from "./axios";

export const getReport = (params, signal) =>
  api.get("/admin/reports", { params, signal }).then((r) => r.data.data);

// The export needs the login token, so it is fetched with Axios and saved from memory.
export async function downloadReportCsv(params) {
  const res = await api.get("/admin/reports/export", { params, responseType: "blob" });
  const url = URL.createObjectURL(res.data);
  const a = document.createElement("a");
  a.href = url;
  a.download = "medislot-report.csv";
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}