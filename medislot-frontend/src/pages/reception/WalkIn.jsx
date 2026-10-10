import { useEffect, useState } from "react";
import ReceptionPage from "../../components/ReceptionPage";
import Field from "../../components/Field";
import { recDoctors, registerWalkIn } from "../../api/staff";
import { getAvailability } from "../../api/doctors";
import { getApiError } from "../../api/errors";
import { todayString } from "../../utils/dates";
import { to12h } from "../../utils/format";

const EMPTY = { fullName: "", mobile: "", email: "", doctorId: "", time: "", reason: "" };

export default function WalkIn() {
  const [doctors, setDoctors] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [slots, setSlots] = useState([]);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => { recDoctors().then(setDoctors).catch((e) => setError(getApiError(e).message)); }, []);

  useEffect(() => {
    setSlots([]);
    if (!form.doctorId) return;
    getAvailability(form.doctorId, todayString()).then((a) => setSlots(a.slots)).catch(() => {});
  }, [form.doctorId]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  function validate() {
    const e = {};
    if (!form.fullName.trim()) e.fullName = "Enter the patient's name.";
    if (!/^\+?[0-9]{10,15}$/.test(form.mobile.trim())) e.mobile = "Use 10-15 digits.";
    if (form.email && !/^\S+@\S+\.\S+$/.test(form.email.trim())) e.email = "Enter a valid email or leave it empty.";
    if (!form.doctorId) e.doctorId = "Choose a doctor.";
    return e;
  }

  async function submit(ev) {
    ev.preventDefault();
    setError(null);
    setResult(null);
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      const body = { fullName: form.fullName.trim(), mobile: form.mobile.trim(), doctorId: Number(form.doctorId) };
      if (form.email.trim()) body.email = form.email.trim();
      if (form.time) body.time = form.time;
      if (form.reason.trim()) body.reason = form.reason.trim();
      setResult(await registerWalkIn(body));
      setForm(EMPTY);
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <ReceptionPage title="Walk-in patient">
      <form onSubmit={submit} noValidate className="max-w-xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field id="w-name" label="Full name" value={form.fullName} onChange={set("fullName")} error={errors.fullName} />
          <Field id="w-mobile" label="Mobile number" type="tel" value={form.mobile} onChange={set("mobile")}
                 error={errors.mobile} hint="An existing patient with this number is reused." />
          <Field id="w-email" label="Email (optional)" type="email" value={form.email} onChange={set("email")} error={errors.email} />
          <div>
            <label htmlFor="w-doc" className="block text-sm font-medium text-slate-700">Doctor</label>
            <select id="w-doc" value={form.doctorId} onChange={set("doctorId")}
                    className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm">
              <option value="">Choose...</option>
              {doctors.map((d) => <option key={d.id} value={d.id}>{d.fullName} ({d.specialization})</option>)}
            </select>
            {errors.doctorId && <p className="mt-1 text-xs text-red-600">{errors.doctorId}</p>}
          </div>
          <div className="sm:col-span-2">
            <label htmlFor="w-time" className="block text-sm font-medium text-slate-700">Slot today</label>
            <select id="w-time" value={form.time} onChange={set("time")} disabled={!form.doctorId}
                    className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm">
              <option value="">First available slot</option>
              {slots.map((s) => <option key={s.start} value={s.start}>{to12h(s.start)}</option>)}
            </select>
            {form.doctorId && slots.length === 0 && (
              <p className="mt-1 text-xs text-amber-700">No free slots are left today for this doctor.</p>
            )}
          </div>
          <div className="sm:col-span-2">
            <label htmlFor="w-reason" className="block text-sm font-medium text-slate-700">Reason (optional)</label>
            <textarea id="w-reason" rows={2} maxLength={500} value={form.reason} onChange={set("reason")}
                      className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            <p className="mt-1 text-xs text-slate-500">Only the doctor can see this.</p>
          </div>
        </div>

        <div aria-live="polite">
          {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
          {result && (
            <div role="status" className="mt-4 rounded-lg bg-green-50 p-4">
              <p className="text-lg font-semibold text-green-800"><span aria-hidden="true">&#10003;</span> Added to the queue</p>
              <p className="mt-1 text-slate-800">{result.patientName} with {result.doctorName}</p>
              <p className="text-sm text-slate-600">
                Slot {to12h(result.appointmentTime)} &middot; Queue number #{result.queueNumber}
              </p>
            </div>
          )}
        </div>

        <button type="submit" disabled={busy}
                className="mt-6 rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
          {busy ? "Registering..." : "Register and check in"}
        </button>
      </form>
    </ReceptionPage>
  );
}