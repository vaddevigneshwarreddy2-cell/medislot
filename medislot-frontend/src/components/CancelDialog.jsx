import { useState } from "react";
import Modal from "./Modal";

export default function CancelDialog({ onClose, onConfirm, busy, error }) {
  const [reason, setReason] = useState("");
  return (
    <Modal title="Cancel this appointment?" onClose={busy ? () => {} : onClose}>
      <p className="mt-2 text-sm text-slate-600">The slot will be released for other patients.</p>
      <label htmlFor="cancel-reason" className="mt-4 block text-sm font-medium text-slate-700">
        Reason (optional)
      </label>
      <textarea
        id="cancel-reason"
        rows={3}
        maxLength={255}
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
      />
      {error && <p role="alert" className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      <div className="mt-5 flex justify-end gap-3">
        <button type="button" onClick={onClose} disabled={busy}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium hover:bg-slate-100 disabled:opacity-60">
          Keep appointment
        </button>
        <button type="button" onClick={() => onConfirm(reason.trim())} disabled={busy}
                className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60">
          {busy ? "Cancelling..." : "Yes, cancel it"}
        </button>
      </div>
    </Modal>
  );
}