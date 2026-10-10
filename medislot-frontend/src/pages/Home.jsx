import { useEffect, useState } from "react";
import api from "../api/axios";

export default function Home() {
  const [state, setState] = useState({ loading: true, error: null, data: null });

  useEffect(() => {
    api
      .get("/health")
      .then((res) => setState({ loading: false, error: null, data: res.data.data }))
      .catch(() =>
        setState({ loading: false, error: "Cannot reach the MediSlot API.", data: null })
      );
  }, []);

  return (
    <section className="max-w-6xl mx-auto px-4 py-16">
      <h1 className="text-4xl font-bold text-slate-900">Find care near you, skip the wait.</h1>
      <p className="mt-3 text-lg text-slate-600 max-w-2xl">
        MediSlot helps you find nearby hospitals, book appointments, and get directions, all in one place.
      </p>

      <div className="mt-10 max-w-md rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">System status</h2>
        {state.loading && <p className="mt-3 text-slate-500">Checking backend...</p>}
        {state.error && (
          <p role="alert" className="mt-3 text-red-600">{state.error}</p>
        )}
        {state.data && (
          <ul className="mt-3 space-y-1 text-sm">
            <li>API: <span className="font-semibold text-green-600">{state.data.status}</span></li>
            <li>Database: <span className="font-semibold">{state.data.database}</span></li>
            <li>Default search radius: <span className="font-semibold">{state.data.defaultSearchRadiusKm} km</span></li>
          </ul>
        )}
      </div>
    </section>
  );
}
