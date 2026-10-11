import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/axios";
import { getApiError } from "../../api/errors";
import { getCurrentLocation } from "../../utils/geolocation";
import { viewLocationUrl } from "../../utils/maps";
import Field from "../../components/Field";

const blank = (v) => (v == null ? "" : String(v));

export default function AdminHospital() {
  const [h, setH] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loc, setLoc] = useState(null);
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(null);

  function fill(d) {
    setH(d);
    setProfile({ name: blank(d.name), phone: blank(d.phone), email: blank(d.email) });
    setLoc({ addressLine: blank(d.addressLine), city: blank(d.city), state: blank(d.state), postalCode: blank(d.postalCode),
             country: blank(d.country), latitude: blank(d.latitude), longitude: blank(d.longitude) });
  }

  useEffect(() => {
    api.get("/admin/hospital").then((r) => fill(r.data.data)).catch((e) => setError(getApiError(e).message));
  }, []);

  async function saveProfile(e) {
    e.preventDefault();
    setMsg(null); setError(null);
    if (!profile.name.trim()) { setErrors({ name: "Enter the hospital name." }); return; }
    setErrors({});
    setBusy("profile");
    try {
      const r = await api.put("/admin/hospital", { name: profile.name.trim(), phone: profile.phone.trim() || null, email: profile.email.trim() || null });
      fill(r.data.data);
      setMsg("Hospital profile saved.");
    } catch (err) { setError(getApiError(err).message); } finally { setBusy(null); }
  }

  async function saveLocation(e) {
    e.preventDefault();
    setMsg(null); setError(null);
    const er = {};
    if (!loc.addressLine.trim()) er.addressLine = "Enter the address.";
    if (!loc.city.trim()) er.city = "Enter the city.";
    if (!loc.state.trim()) er.state = "Enter the state.";
    const lat = Number(loc.latitude), lng = Number(loc.longitude);
    if (loc.latitude === "" || !Number.isFinite(lat) || lat < -90 || lat > 90) er.latitude = "Latitude must be between -90 and 90.";
    if (loc.longitude === "" || !Number.isFinite(lng) || lng < -180 || lng > 180) er.longitude = "Longitude must be between -180 and 180.";
    setErrors(er);
    if (Object.keys(er).length) return;
    setBusy("location");
    try {
      const r = await api.put("/admin/hospital/location", {
        addressLine: loc.addressLine.trim(), city: loc.city.trim(), state: loc.state.trim(),
        postalCode: loc.postalCode.trim() || null, country: loc.country.trim() || null, latitude: lat, longitude: lng });
      fill(r.data.data);
      setMsg("Hospital location saved. Patients will see the new position in searches and directions.");
    } catch (err) { setError(getApiError(err).message); } finally { setBusy(null); }
  }

  async function useHere() {
    setError(null);
    try {
      const p = await getCurrentLocation();
      setLoc((l) => ({ ...l, latitude: p.lat.toFixed(7), longitude: p.lng.toFixed(7) }));
      setMsg("Coordinates filled from this device. Check them on the map, then save.");
    } catch (err) { setError(err.message); }
  }

  if (!profile && !error) return <p className="p-8 text-center text-slate-500">Loading...</p>;
  const set = (setter) => (k) => (e) => setter((o) => ({ ...o, [k]: e.target.value }));
  const sp = set(setProfile), sl = set(setLoc);

  return (
    <section className="mx-auto max-w-3xl px-4 py-10">
      <Link to="/admin" className="text-sm font-medium text-brand-600 hover:underline">&larr; Dashboard</Link>
      <h1 className="mt-2 text-3xl font-bold text-slate-900">{h ? h.name : "Hospital profile"}</h1>
      <div aria-live="polite">
        {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        {msg && <p role="status" className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{msg}</p>}
      </div>

      {profile && (
        <>
          <form onSubmit={saveProfile} noValidate className="mt-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-900">Profile</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2"><Field id="h-name" label="Hospital name" value={profile.name} onChange={sp("name")} error={errors.name} /></div>
              <Field id="h-phone" label="Phone" value={profile.phone} onChange={sp("phone")} />
              <Field id="h-email" label="Email" type="email" value={profile.email} onChange={sp("email")} />
            </div>
            <button type="submit" disabled={busy !== null} className="mt-6 rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
              {busy === "profile" ? "Saving..." : "Save profile"}
            </button>
          </form>

          <form onSubmit={saveLocation} noValidate className="mt-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-900">Address and map location</h2>
            <p className="text-sm text-slate-500">Patients find you by these coordinates, and directions lead to them.</p>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2"><Field id="l-addr" label="Address" value={loc.addressLine} onChange={sl("addressLine")} error={errors.addressLine} /></div>
              <Field id="l-city" label="City" value={loc.city} onChange={sl("city")} error={errors.city} />
              <Field id="l-state" label="State" value={loc.state} onChange={sl("state")} error={errors.state} />
              <Field id="l-pin" label="Postal code" value={loc.postalCode} onChange={sl("postalCode")} />
              <Field id="l-country" label="Country" value={loc.country} onChange={sl("country")} />
              <Field id="l-lat" label="Latitude" value={loc.latitude} onChange={sl("latitude")} error={errors.latitude} hint="Example: 17.4126000" />
              <Field id="l-lng" label="Longitude" value={loc.longitude} onChange={sl("longitude")} error={errors.longitude} hint="Example: 78.4482000" />
            </div>
            <div className="mt-4 flex flex-wrap gap-3 text-sm">
              <button type="button" onClick={useHere} className="rounded-lg border border-slate-300 px-4 py-2 font-medium hover:bg-slate-100">
                Use my current location
              </button>
              {Number.isFinite(Number(loc.latitude)) && Number.isFinite(Number(loc.longitude)) && loc.latitude !== "" && loc.longitude !== "" && (
                <a href={viewLocationUrl({ latitude: loc.latitude, longitude: loc.longitude })} target="_blank" rel="noopener noreferrer"
                   className="rounded-lg border border-slate-300 px-4 py-2 font-medium hover:bg-slate-100">Check on Google Maps</a>
              )}
            </div>
            <button type="submit" disabled={busy !== null} className="mt-6 rounded-lg bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
              {busy === "location" ? "Saving..." : "Save location"}
            </button>
          </form>
        </>
      )}
    </section>
  );
}