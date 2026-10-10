import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { dashboardPath } from "../auth/roles";
import { getApiError } from "../api/errors";
import AuthCard from "../components/AuthCard";
import Field from "../components/Field";

function destination(user, location) {
  const from = location.state?.from;
  if (from?.pathname && user.role === "PATIENT") return `${from.pathname}${from.search || ""}`;
  return dashboardPath(user.role);
}

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to={destination(user, location)} replace />;

  async function onSubmit(e) {
    e.preventDefault();
    setError(null);
    if (!form.email.trim() || !form.password) {
      setError("Enter your email and password.");
      return;
    }
    setSubmitting(true);
    try {
      const u = await login(form.email.trim(), form.password);
      navigate(destination(u, location), { replace: true });
    } catch (err) {
      const { code, message } = getApiError(err);
      if (code === "ACCOUNT_NOT_VERIFIED") {
        navigate("/verify", { state: { email: form.email.trim() } });
        return;
      }
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthCard
      title="Log in"
      subtitle="Welcome back to MediSlot."
      footer={<>New here? <Link to="/register" className="text-brand-600 font-medium">Create an account</Link></>}
    >
      {location.state?.message && (
        <p role="status" className="mb-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">
          {location.state.message}
        </p>
      )}
      {error && (
        <p role="alert" className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
      )}
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <Field id="email" label="Email" type="email" autoComplete="email" value={form.email}
               onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <Field id="password" label="Password" type="password" autoComplete="current-password"
               value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <button type="submit" disabled={submitting}
                className="w-full rounded-lg bg-brand-600 px-4 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
          {submitting ? "Logging in..." : "Log in"}
        </button>
      </form>
    </AuthCard>
  );
}