import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { isCancel } from "axios";
import { downloadReportCsv, getReport } from "../../api/reports";
import { getApiError } from "../../api/errors";
import StackedBars from "../../components/charts/StackedBars";
import HorizontalBars from "../../components/charts/HorizontalBars";
import { parseDate, toDateString } from "../../utils/dates";

function daysAgo(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return toDateString(d);
}

const GRANULARITIES = [["auto", "Auto"], ["day", "Daily"], ["week", "Weekly"], ["month", "Monthly"]];

function Card({ label, value, note }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-3xl font-bold text-slate-900">{value}</p>
      <p className="text-sm text-slate-500">{label}</p>
      {note && <p className="text-xs text-slate-400">{note}</p>}
    </div>
  );
}

export default function AdminReports() {
  const [from, setFrom] = useState(daysAgo(29));
  const [to, setTo] = useState(daysAgo(0));
  const [granularity, setGranularity] = useState("auto");
  const [report, setReport] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  const rangeProblem = !from || !to ? "Choose both dates."
    : from > to ? "The start date must not be after the end date."
    : (parseDate(to) - parseDate(from)) / 86400000 > 365 ? "A report can cover at most 366 days." : null;

  useEffect(() => {
    if (rangeProblem) { setLoading(false); return undefined; }
    const c = new AbortController();
    setLoading(true);
    setError(null);
    getReport({ from, to, granularity }, c.signal)
      .then((r) => { setReport(r); setLoading(false); })
      .catch((err) => {
        if (isCancel(err)) return;
        setError(getApiError(err).message);
        setLoading(false);
      });
    return () => c.abort();
  }, [from, to, granularity, rangeProblem]);

  async function exportCsv() {
    setExporting(true);
    setError(null);
    try {
      await downloadReportCsv({ from, to, granularity });
    } catch (err) {
      setError(err?.response?.status === 429 ? getApiError(err).message : "Could not download the CSV. Please try again.");
    } finally {
      setExporting(false);
    }
  }

  const s = report?.summary;
  const inputCls = "rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500";

  return (
    <section className="mx-auto max-w-5xl px-4 py-10">
      <Link to="/admin" className="text-sm font-medium text-brand-600 hover:underline">&larr; Dashboard</Link>
      <h1 className="mt-2 text-3xl font-bold text-slate-900">Reports</h1>
      <p className="text-slate-600">Counts only. No patient details appear in reports or in the CSV.</p>

      <div className="mt-6 flex flex-wrap items-end gap-3">
        <div><label htmlFor="r-from" className="block text-xs font-medium text-slate-600">From</label>
          <input id="r-from" type="date" className={inputCls} value={from} onChange={(e) => setFrom(e.target.value)} /></div>
        <div><label htmlFor="r-to" className="block text-xs font-medium text-slate-600">To</label>
          <input id="r-to" type="date" className={inputCls} value={to} onChange={(e) => setTo(e.target.value)} /></div>
        <div role="group" aria-label="Group by" className="flex gap-2">
          {GRANULARITIES.map(([value, label]) => (
            <button key={value} type="button" onClick={() => setGranularity(value)} aria-pressed={granularity === value}
                    className={`rounded-full border px-4 py-2 text-sm font-medium ${
                      granularity === value ? "border-brand-600 bg-brand-600 text-white" : "border-slate-300 bg-white hover:bg-slate-100"}`}>
              {label}
            </button>
          ))}
        </div>
        <button type="button" onClick={exportCsv} disabled={exporting || Boolean(rangeProblem)}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium hover:bg-slate-100 disabled:opacity-60">
          {exporting ? "Preparing..." : "Download CSV"}
        </button>
      </div>

      <div aria-live="polite">
        {rangeProblem && <p role="alert" className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">{rangeProblem}</p>}
        {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        {loading && <p className="mt-6 text-center text-slate-500">Loading...</p>}
      </div>

      {report && !rangeProblem && (
        <>
          <p className="mt-4 text-xs text-slate-500">
            {report.from} to {report.to}, grouped by {report.granularity}. Held, expired and replaced bookings are not counted.
          </p>
          <div className="mt-3 grid gap-3 sm:grid-cols-4">
            <Card label="Appointments" value={s.total} />
            <Card label="Completed" value={s.completed} />
            <Card label="Cancelled" value={s.cancelled} note={`${s.cancellationRate}% of bookings`} />
            <Card label="No-shows" value={s.noShow} note={`${s.noShowRate}% of bookings`} />
            <Card label="Checked in" value={s.checkedIn} />
            <Card label="Average wait" value={s.avgWaitMinutes == null ? "No data" : `${s.avgWaitMinutes} min`}
                  note={`check-in to consultation, ${s.waitSamples} visit${s.waitSamples === 1 ? "" : "s"}`} />
          </div>

          <h2 className="mt-8 text-sm font-semibold uppercase tracking-wide text-slate-500">Appointments over time</h2>
          <div className="mt-2 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <StackedBars data={report.series} unit={report.granularity} />
          </div>

          <HorizontalBars title="By doctor" rows={report.doctors} />
          <HorizontalBars title="By department" rows={report.departments} />
        </>
      )}
    </section>
  );
}