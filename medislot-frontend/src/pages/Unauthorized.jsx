import { Link } from "react-router-dom";

export default function Unauthorized() {
  return (
    <section className="max-w-6xl mx-auto px-4 py-24 text-center">
      <h1 className="text-4xl font-bold text-slate-900">Access denied</h1>
      <p className="mt-3 text-slate-600">Your account does not have permission to view that page.</p>
      <Link to="/" className="mt-6 inline-block rounded-lg bg-brand-600 px-5 py-2 text-white hover:bg-brand-700">
        Back to home
      </Link>
    </section>
  );
}
