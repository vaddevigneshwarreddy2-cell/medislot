import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { completeConsultation, getConsultation, saveConsultation } from "../../api/clinical";
import { getApiError } from "../../api/errors";
import PdfButton from "../../components/PdfButton";
import { formatLong, parseDate, toDateString } from "../../utils/dates";
import { to12h } from "../../utils/format";

const BLANK = { medicineName: "", dosage: "", frequency: "", duration: "", instructions: "" };
const MAX_MEDICINES = 20;

function toForm(c) {
  return {
    notes: c.notes || "",
    diagnosis: c.diagnosis || "",
    instructions: c.instructions || "",
    followUpDate: c.followUpDate || "",
    medicines: c.medicines.map((m) => ({
      medicineName: m.medicineName, dosage: m.dosage, frequency: m.frequency,
      duration: m.duration, instructions: m.instructions || "",
    })),
  };
}

function buildBody(form) {
  const medicines = form.medicines
    .filter((m) => Object.values(m).some((v) => v.trim()))
    .map((m) => ({ ...m, instructions: m.instructions.trim() || null }));
  return {
    notes: form.notes, diagnosis: form.diagnosis, instructions: form.instructions,
    followUpDate: form.followUpDate || null, medicines,
  };
}

function addDays(dateString, n) {
  const d = parseDate(dateString);
  d.setDate(d.getDate() + n);
  return toDateString(d);
}

function validate(form, appointmentDate, forComplete) {
  for (const m of form.medicines) {
    const any = Object.values(m).some((v) => v.trim());
    if (any && (!m.medicineName.trim() || !m.dosage.trim() || !m.frequency.trim() || !m.duration.trim())) {
      return "Each medicine needs a name, dosage, frequency and duration (or remove the row).";
    }
  }
  if (form.followUpDate) {
    if (form.followUpDate <= appointmentDate) return "The follow-up date must be after the appointment date.";
    if (form.followUpDate > addDays(appointmentDate, 365)) return "The follow-up date must be within a year.";
  }
  if (forComplete && !form.notes.trim() && !form.diagnosis.trim()) {
    return "Add consultation notes or a diagnosis before completing.";
  }
  return null;
}

export default function Consultation() {
  const { appointmentId } = useParams();
  const [c, setC] = useState(null);
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [busy, setBusy] = useState(null); // "save" | "complete" | null
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    setLoading(true);
    getConsultation(appointmentId)
      .then((r) => { setC(r); setForm(toForm(r)); setLoading(false); })
      .catch((e) => { setLoadError(getApiError(e)); setLoading(false); });
  }, [appointmentId]);

  useEffect(() => {
    if (!dirty) return undefined;
    const warn = (e) => { e.preventDefault(); e.returnValue = ""; };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  if (loading) return <p className="p-8 text-center text-slate-500">Loading...</p>;
  if (loadError || !c) {
    return (
      <section className="mx-auto max-w-xl px-4 py-16 text-center">
        <p role="alert" className="text-lg text-slate-700">{loadError?.message || "Consultation not found."}</p>
        <Link to="/doctor/queue" className="mt-4 inline-block font-medium text-brand-600 hover:underline">Back to the queue</Link>
      </section>
    );
  }

  const editable = c.editable;
  const set = (k) => (e) => { setForm({ ...form, [k]: e.target.value }); setDirty(true); };
  const setMed = (i, k, v) => {
    setForm({ ...form, medicines: form.medicines.map((m, j) => (j === i ? { ...m, [k]: v } : m)) });
    setDirty(true);
  };
  const addMed = () => {
    if (form.medicines.length >= MAX_MEDICINES) return;
    setForm({ ...form, medicines: [...form.medicines, { ...BLANK }] });
    setDirty(true);
  };
  const removeMed = (i) => { setForm({ ...form, medicines: form.medicines.filter((_, j) => j !== i) }); setDirty(true); };

  async function run(kind) {
    setError(null);
    setMsg(null);
    const problem = validate(form, c.appointmentDate, kind === "complete");
    if (problem) { setError(problem); return; }
    if (kind === "complete" &&
        !window.confirm("Complete this consultation? The record will be locked and the patient will see the prescription.")) return;
    setBusy(kind);
    try {
      const body = buildBody(form);
      const r = kind === "complete"
        ? await completeConsultation(c.appointmentId, body)
        : await saveConsultation(c.appointmentId, body);
      setC(r);
      setForm(toForm(r));
      setDirty(false);
      setMsg(kind === "complete" ? "Consultation completed." : "Draft saved.");
    } catch (err) {
      const e = getApiError(err);
      setError(e.message);
    } finally {
      setBusy(null);
    }
  }

  const inputCls = "block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-slate-50 disabled:text-slate-600";
  const maxFollow = addDays(c.appointmentDate, 365);
  const minFollow = addDays(c.appointmentDate, 1);

  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <Link to="/doctor/queue" className="text-sm font-medium text-brand-600 hover:underline">&larr; Today's queue</Link>
      <div className="mt-2 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Consultation</h1>
          <p className="text-slate-600">
            {c.patientName} &middot; {formatLong(c.appointmentDate)} at {to12h(c.appointmentTime)}
          </p>
        </div>
        {!editable && c.prescriptionId && <PdfButton role="DOCTOR" id={c.prescriptionId} />}
      </div>

      {c.reason && (
        <p className="mt-4 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700">
          <span className="font-semibold">Reason for visit:</span> {c.reason}
        </p>
      )}
      {!editable && (
        <p role="status" className="mt-4 rounded-lg bg-green-50 px-4 py-3 font-medium text-green-800">
          This consultation is completed and locked.
        </p>
      )}

      <div aria-live="polite">
        {msg && <p role="status" className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}
        {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      </div>

      <div className="mt-6 space-y-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <div>
          <label htmlFor="notes" className="block text-sm font-medium text-slate-700">Consultation notes (private)</label>
          <textarea id="notes" rows={5} maxLength={5000} disabled={!editable} className={inputCls}
                    value={form.notes} onChange={set("notes")} />
          <p className="mt-1 text-xs text-slate-500">Only you can see these notes. They are not shown to the patient.</p>
        </div>

        <div>
          <label htmlFor="diagnosis" className="block text-sm font-medium text-slate-700">Diagnosis</label>
          <textarea id="diagnosis" rows={2} maxLength={2000} disabled={!editable} className={inputCls}
                    value={form.diagnosis} onChange={set("diagnosis")} />
        </div>

        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Prescription</h2>
          {form.medicines.length === 0 && (
            <p className="mt-2 text-sm text-slate-500">{editable ? "No medicines added." : "No medicines were prescribed."}</p>
          )}
          <div className="mt-3 space-y-3">
            {form.medicines.map((m, i) => (
              <div key={i} className="rounded-lg border border-slate-200 p-3">
                <div className="grid gap-2 sm:grid-cols-4">
                  {[["medicineName", "Medicine"], ["dosage", "Dosage"], ["frequency", "Frequency"], ["duration", "Duration"]].map(([k, label]) => (
                    <div key={k}>
                      <label htmlFor={`m${i}-${k}`} className="block text-xs font-medium text-slate-600">{label}</label>
                      <input id={`m${i}-${k}`} maxLength={k === "medicineName" ? 150 : 100} disabled={!editable}
                             className={inputCls} value={m[k]} onChange={(e) => setMed(i, k, e.target.value)} />
                    </div>
                  ))}
                </div>
                <div className="mt-2 flex items-end gap-2">
                  <div className="flex-1">
                    <label htmlFor={`m${i}-instructions`} className="block text-xs font-medium text-slate-600">Notes (optional)</label>
                    <input id={`m${i}-instructions`} maxLength={255} disabled={!editable} className={inputCls}
                           value={m.instructions} onChange={(e) => setMed(i, "instructions", e.target.value)} />
                  </div>
                  {editable && (
                    <button type="button" onClick={() => removeMed(i)} className="pb-2 text-sm font-medium text-red-600 hover:underline">
                      Remove
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
          {editable && (
            <button type="button" onClick={addMed} disabled={form.medicines.length >= MAX_MEDICINES}
                    className="mt-3 text-sm font-medium text-brand-600 hover:underline disabled:text-slate-400">
              + Add medicine
            </button>
          )}
        </div>

        <div>
          <label htmlFor="instructions" className="block text-sm font-medium text-slate-700">Instructions for the patient</label>
          <textarea id="instructions" rows={3} maxLength={2000} disabled={!editable} className={inputCls}
                    value={form.instructions} onChange={set("instructions")} />
        </div>

        <div>
          <label htmlFor="followUp" className="block text-sm font-medium text-slate-700">Follow-up date (optional)</label>
          <input id="followUp" type="date" min={minFollow} max={maxFollow} disabled={!editable}
                 className={inputCls + " max-w-xs"} value={form.followUpDate} onChange={set("followUpDate")} />
        </div>

        {editable && (
          <div className="flex flex-wrap gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={() => run("save")} disabled={busy !== null}
                    className="rounded-lg border border-slate-300 px-5 py-2 font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60">
              {busy === "save" ? "Saving..." : "Save draft"}
            </button>
            <button type="button" onClick={() => run("complete")} disabled={busy !== null}
                    className="rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
              {busy === "complete" ? "Completing..." : "Complete consultation"}
            </button>
          </div>
        )}
      </div>
    </section>
  );
}