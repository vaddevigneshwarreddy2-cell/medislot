import { Link } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";

export default function DashboardShell({ title, items }) {
  const { user } = useAuth();
  return (
    <section className="max-w-6xl mx-auto px-4 py-10">
      <h1 className="text-3xl font-bold text-slate-900">{title}</h1>
      <p className="mt-1 text-slate-600">
        Signed in as <span className="font-semibold">{user.fullName}</span>{" "}
        <span className="ml-1 rounded-full bg-brand-100 px-2 py-0.5 text-xs font-semibold text-brand-700">
          {user.role}
        </span>
      </p>
      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((item) => {
          const body = (
            <>
              <h2 className="font-semibold text-slate-800">{item.label}</h2>
              <p className="mt-1 text-sm text-slate-500">{item.note}</p>
            </>
          );
          return item.to ? (
            <Link key={item.label} to={item.to}
                  className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm hover:border-brand-500">
              {body}
            </Link>
          ) : (
            <div key={item.label} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">{body}</div>
          );
        })}
      </div>
    </section>
  );
}