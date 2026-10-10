import DashboardShell from "./DashboardShell";

export function PatientDashboard() {
  return (
    <DashboardShell
      title="Patient dashboard"
      items={[
        { label: "Find a hospital", note: "Search near you or by name", to: "/hospitals" },
        { label: "Find a doctor", note: "By specialization, name or location", to: "/doctors" },
        { label: "My appointments", note: "Upcoming and past visits, QR codes", to: "/patient/appointments" },
        { label: "Prescriptions", note: "Coming in Phase 7" },
      ]}
    />
  );
}

export function DoctorDashboard() {
  return (
    <DashboardShell
      title="Doctor dashboard"
      items={[
        { label: "Today's queue", note: "Call, start and complete consultations", to: "/doctor/queue" },
        { label: "Availability and leave", note: "Working hours and days off", to: "/doctor/schedule" },
        { label: "Consultation notes and prescriptions", note: "Coming in Phase 7" },
      ]}
    />
  );
}

export function ReceptionDashboard() {
  return (
    <DashboardShell
      title="Reception dashboard"
      items={[
        { label: "Overview", note: "Today at a glance", to: "/reception/overview" },
        { label: "QR check-in", note: "Scan or enter a patient's code", to: "/reception/check-in" },
        { label: "Queue board", note: "Who is waiting for which doctor", to: "/reception/queue" },
        { label: "Walk-ins", note: "Register a patient at the desk", to: "/reception/walk-in" },
        { label: "Appointments", note: "Search, check in, cancel or no-show", to: "/reception/appointments" },
      ]}
    />
  );
}

export function AdminDashboard() {
  return (
    <DashboardShell
      title="Hospital admin dashboard"
      items={[
        { label: "Doctors", note: "Add doctors, set schedules and leave", to: "/admin/doctors" },
        { label: "Departments", note: "Manage departments", to: "/admin/departments" },
        { label: "Holidays", note: "Days the hospital is closed", to: "/admin/holidays" },
        { label: "Hospital profile and location", note: "Admin screen coming later" },
        { label: "Reports", note: "Coming in Phase 9" },
      ]}
    />
  );
}