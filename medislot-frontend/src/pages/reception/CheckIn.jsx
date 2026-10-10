import { useEffect, useRef, useState } from "react";
import ReceptionPage from "../../components/ReceptionPage";
import QrScanner, { scannerSupported } from "../../components/QrScanner";
import { checkInByCode } from "../../api/staff";
import { getApiError } from "../../api/errors";
import { to12h } from "../../utils/format";

export default function CheckIn() {
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const input = useRef(null);

  useEffect(() => { input.current?.focus(); }, [result]);

  async function submit(value) {
    const v = (value ?? code).trim();
    if (!v) { setError("Enter or scan a code."); return; }
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      setResult(await checkInByCode(v));
      setCode("");
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <ReceptionPage title="QR check-in">
      <div className="max-w-xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <form onSubmit={(e) => { e.preventDefault(); submit(); }}>
          <label htmlFor="qr-code" className="block text-sm font-medium text-slate-700">Patient's check-in code</label>
          <div className="mt-1 flex gap-2">
            <input id="qr-code" ref={input} value={code} onChange={(e) => setCode(e.target.value)}
                   placeholder="MS1.123.abcdef..." autoComplete="off" spellCheck={false} maxLength={100}
                   className="block flex-1 rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
            <button type="submit" disabled={busy}
                    className="rounded-lg bg-brand-600 px-5 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60">
              {busy ? "Checking..." : "Check in"}
            </button>
          </div>
          <p className="mt-1 text-xs text-slate-500">A USB barcode scanner will type the code here and press Enter for you.</p>
        </form>

        {scannerSupported() && (
          <div className="mt-4">
            <QrScanner onCode={(text) => { setCode(text); submit(text); }} />
          </div>
        )}

        <div aria-live="polite">
          {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
          {result && (
            <div role="status" className="mt-4 rounded-lg bg-green-50 p-4">
              <p className="text-lg font-semibold text-green-800"><span aria-hidden="true">&#10003;</span> Checked in</p>
              <p className="mt-1 text-slate-800">{result.patientName}</p>
              <p className="text-sm text-slate-600">{result.doctorName} &middot; {to12h(result.appointmentTime)}</p>
              <p className="mt-2 text-sm font-medium text-slate-800">
                Queue number #{result.queueNumber}
                {result.position != null ? ` - position ${result.position} for this doctor` : ""}
              </p>
            </div>
          )}
        </div>
      </div>
      <p className="mt-4 text-sm text-slate-500">
        If the patient has no code, find them under <span className="font-medium">Appointments</span> and check them in from there.
      </p>
    </ReceptionPage>
  );
}