import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axios";
import { getApiError } from "../api/errors";
import AuthCard from "../components/AuthCard";
import Field from "../components/Field";

function validate(f) {
  const errors = {};
  if (!f.fullName.trim()) errors.fullName = "Enter your full name.";
  if (!/^\S+@\S+\.\S+$/.test(f.email.trim())) errors.email = "Enter a valid email address.";
  if (!/^\+?[0-9]{10,15}$/.test(f.mobile.trim())) errors.mobile = "Use 10-15 digits, optional leading +.";
  if (f.password.length < 8 || f.password.length > 72) {
    errors.password = "Password must be 8-72 characters.";
  } else if (!/[a-z]/.test(f.password) || !/[A-Z]/.test(f.password) || !/\d/.test(f.password)) {
    errors.password = "Include an uppercase letter, a lowercase letter and a digit.";
  }
  if (f.confirm !== f.password) errors.confirm = "Passwords do not match.";
  return errors;
}

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: "", email: "", mobile: "", password: "", confirm: "" });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  async function onSubmit(e) {
    e.preventDefault();
    setServerError(null);
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      await api.post("/auth/register", {
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        mobile: form.mobile.trim(),
        password: form.password,
      });
      navigate("/verify", { state: { email: form.email.trim().toLowerCase() } });
    } catch (err) {
      setServerError(getApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthCard
      title="Create your account"
      subtitle="Book appointments at hospitals near you."
      footer={<>Already registered? <Link to="/login" className="text-brand-600 font-medium">Log in</Link></>}
    >
      {serverError && (
        <p role="alert" className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{serverError}</p>
      )}
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <Field id="fullName" label="Full name" autoComplete="name" value={form.fullName}
               onChange={set("fullName")} error={errors.fullName} />
        <Field id="email" label="Email" type="email" autoComplete="email" value={form.email}
               onChange={set("email")} error={errors.email} />
        <Field id="mobile" label="Mobile number" type="tel" autoComplete="tel" value={form.mobile}
               onChange={set("mobile")} error={errors.mobile} hint="Example: 9876543210" />
        <Field id="password" label="Password" type="password" autoComplete="new-password" value={form.password}
               onChange={set("password")} error={errors.password}
               hint="At least 8 characters with upper case, lower case and a digit." />
        <Field id="confirm" label="Confirm password" type="password" autoComplete="new-password"
               value={form.confirm} onChange={set("confirm")} error={errors.confirm} />
        <button type="submit" disabled={submitting}
                className="w-full rounded-lg bg-brand-600 px-4 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-60">
          {submitting ? "Creating account..." : "Register"}
        </button>
      </form>
    </AuthCard>
  );
}
