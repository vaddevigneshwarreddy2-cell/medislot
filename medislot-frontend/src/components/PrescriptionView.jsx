import PdfButton from "./PdfButton";
import { formatLong } from "../utils/dates";

export default function PrescriptionView({ rx, role }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Prescription</h1>
          <p className="text-sm text-slate-500">No. {rx.id} &middot; Appointment #{rx.appointmentId}</p>
        </div>
        <PdfButton role={role} id={rx.id} />
      </div>

      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Doctor</p>
          <p className="font-medium text-slate-900">{rx.doctorName}</p>
          <p className="text-sm text-slate-600">{rx.specialization}{rx.qualification ? ` | ${rx.qualification}` : ""}</p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Hospital</p>
          <p className="font-medium text-slate-900">{rx.hospitalName}</p>
          <p className="text-sm text-slate-600">{rx.hospitalAddress}</p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Patient</p>
          <p className="font-medium text-slate-900">{rx.patientName}</p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Date</p>
          <p className="font-medium text-slate-900">{formatLong(rx.date)}</p>
        </div>
      </div>

      {rx.diagnosis && (
        <div className="mt-6">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Diagnosis</h2>
          <p className="mt-1 whitespace-pre-wrap text-slate-800">{rx.diagnosis}</p>
        </div>
      )}

      <h2 className="mt-6 text-sm font-semibold uppercase tracking-wide text-slate-500">Medicines</h2>
      {rx.medicines.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">No medicines were prescribed.</p>
      ) : (
        <div className="mt-2 overflow-x-auto rounded-lg border border-slate-200">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 text-left text-slate-500">
              <tr>
                <th className="px-3 py-2">Medicine</th><th className="px-3 py-2">Dosage</th>
                <th className="px-3 py-2">Frequency</th><th className="px-3 py-2">Duration</th>
                <th className="px-3 py-2">Notes</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rx.medicines.map((m, i) => (
                <tr key={i}>
                  <td className="px-3 py-2 font-medium text-slate-800">{m.medicineName}</td>
                  <td className="px-3 py-2">{m.dosage}</td>
                  <td className="px-3 py-2">{m.frequency}</td>
                  <td className="px-3 py-2">{m.duration}</td>
                  <td className="px-3 py-2 text-slate-600">{m.instructions || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {rx.instructions && (
        <div className="mt-6">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Instructions</h2>
          <p className="mt-1 whitespace-pre-wrap text-slate-800">{rx.instructions}</p>
        </div>
      )}

      {rx.followUpDate && (
        <p className="mt-6 rounded-lg bg-sky-50 px-4 py-3 font-medium text-sky-800">
          Follow-up date: {formatLong(rx.followUpDate)}
        </p>
      )}
    </div>
  );
}