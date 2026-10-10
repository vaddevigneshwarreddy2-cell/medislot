import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import api from "../api/axios";
import { getApiError } from "../api/errors";
import AuthCard from "../components/AuthCard";
import Field from "../components/Field";

export default function Verify() {
  const location = useLocation();
  const navigate = useNavigate();
  const [email, setEmail] = useState(location.state?.email || "");
  const [code, setCode] = useState("");
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return;
    const t = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [cooldown]);

  async function onSubmit(e) {
    e.preventDefault();
    setError(null);
    setInfo(null);
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) { setError("Enter a valid email address."); return; }
    if (!/^\d{6}$/.test(code)) { setError("Enter the 6-digit code."); return; }
    setSubmitting(true);
    try {
      await api.post("/auth/verify", { email: email.trim(), code });
      navigate("/login", { state: { message: "Account verified. Please log in." } });
    } catch (err) {
      setError(getApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  }

  async function resend() {
    setError(null);
    setInfo(null);
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) { setError("Enter your email first."); return; }
    try {
      await api.post("/auth/resend-code", { email: email.trim() });
      setInfo("If the account exists and is unverified, a new code has been sent.");
      setCooldown(60);
    } catch (err) {
      setError(getApiError(err).message);
    }
  }

  return (
    <AuthCard
      title="Verify your account"
      subtitle="Enter the 6-digit code we sent you. It expires in 10 minutes."
      footer={<Link to="/login" className="text-brand-600 font-medium">Back to log in</Link>}
    >
      <p className="mb-4 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800">
        Demo mode: the code is printed in the backend terminal (look for "DEV OTP").
      </p>
      {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      {info && <p role="status" className="mb-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{info}</p>}
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <Field id="email" label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <Field id="code" label="Verification code" inputMode="numeric" maxLength={6} autoComplete="one-time-code"
               value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))} />
        <button type="submit" disabled={submitting}
                className="w-full rounded-lg bg-brand-600 px-4 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
          {submitting ? "Verifying..." : "Verify"}
        </button>
      </form>
      <button type="button" onClick={resend} disabled={cooldown > 0}
              className="mt-4 w-full text-sm font-medium text-brand-600 hover:underline disabled:text-slate-400 disabled:no-underline">
        {cooldown > 0 ? `Resend code in ${cooldown}s` : "Resend code"}
      </button>
    </AuthCard>
  );
}
