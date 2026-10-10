import { Navigate, Outlet, useLocation } from "react-router-dom";
import { ROLE_HOME, useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ roles }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="p-8 text-center text-gray-500">Loading...</div>;
  }
  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (roles && !roles.includes(user.role)) {
    // Logged in, but wrong role: send them to their own dashboard.
    return <Navigate to={ROLE_HOME[user.role] || "/login"} replace />;
  }
  return <Outlet />;
}