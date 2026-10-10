import { useState } from "react";
import { downloadPdf } from "../api/clinical";

export default function PdfButton({ role, id, className = "" }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function go() {
    setBusy(true);
    setError(null);
    try {
      await downloadPdf(role, id);
    } catch {
      setError("Could not download the PDF. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <span className="inline-block">
      <button type="button" onClick={go} disabled={busy}
              className={`rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60 ${className}`}>
        {busy ? "Preparing PDF..." : "Download PDF"}
      </button>
      {error && <span role="alert" className="mt-1 block text-xs text-red-700">{error}</span>}
    </span>
  );
}