import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { dashboardPath } from "../auth/roles";
import NotificationBell from "./NotificationBell";

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  return (
    <header className="bg-white border-b border-slate-200">
      <nav className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 text-xl font-bold text-brand-600">
          <span aria-hidden="true">&#127973;</span> MediSlot
        </Link>
        <div className="flex items-center gap-4 text-sm font-medium text-slate-600">
          <Link to="/" className="hover:text-brand-600">Home</Link>
          <Link to="/hospitals" className="hover:text-brand-600">Find hospitals</Link>
          <Link to="/doctors" className="hover:text-brand-600">Find doctors</Link>
          {user ? (
            <>
              <Link to={dashboardPath(user.role)} className="hover:text-brand-600">Dashboard</Link>
              {user.role === "PATIENT" && (
                <>
                  <Link to="/patient/appointments" className="hover:text-brand-600">My appointments</Link>
                  <Link to="/patient/prescriptions" className="hover:text-brand-600">Prescriptions</Link>
                </>
              )}
              <NotificationBell />
              <span className="hidden sm:inline text-slate-400">|</span>
              <span className="hidden sm:inline text-slate-700">{user.fullName}</span>
              <button
                onClick={handleLogout}
                className="rounded-lg border border-slate-300 px-3 py-1.5 hover:bg-slate-100"
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="hover:text-brand-600">Log in</Link>
              <Link to="/register" className="rounded-lg bg-brand-600 px-3 py-1.5 text-white hover:bg-brand-700">
                Register
              </Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}