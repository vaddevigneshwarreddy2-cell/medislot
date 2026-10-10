import { useAuth } from "../context/AuthContext";

function Shell({ title, description }) {
  const { user, logout } = useAuth();
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow px-6 py-3 flex items-center justify-between">
        <span className="font-semibold text-blue-700">MediSlot</span>
        <div className="flex items-center gap-4 text-sm">
          <span className="text-gray-600">
            {user?.fullName || user?.email} <span className="text-xs text-gray-400">({user?.role})</span>
          </span>
          <button onClick={logout} className="text-red-600">Log out</button>
        </div>
      </header>
      <main className="max-w-3xl mx-auto p-6">
        <h1 className="text-2xl font-semibold mb-2">{title}</h1>
        <p className="text-gray-600">{description}</p>
      </main>
    </div>
  );
}

export const PatientDashboard = () => (
  <Shell title="Patient Dashboard" description="Appointments and nearby hospitals will appear here." />
);
export const DoctorDashboard = () => (
  <Shell title="Doctor Dashboard" description="Your schedule and consultations will appear here." />
);
export const ReceptionDashboard = () => (
  <Shell title="Reception Dashboard" description="Check-in and queue management will appear here." />
);
export const AdminDashboard = () => (
  <Shell title="Hospital Admin Dashboard" description="Hospital, doctor and report management will appear here." />
);
export const SuperAdminDashboard = () => (
  <Shell title="Super Admin Dashboard" description="Platform-wide management (future)." />
);