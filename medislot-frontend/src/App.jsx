import { Routes, Route, Navigate } from "react-router-dom";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./auth/ProtectedRoute";
import { ROLES } from "./auth/roles";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Verify from "./pages/Verify";
import Unauthorized from "./pages/Unauthorized";
import NotFound from "./pages/NotFound";
import HospitalSearch from "./pages/HospitalSearch";
import HospitalDetails from "./pages/HospitalDetails";
import DoctorSearch from "./pages/DoctorSearch";
import DoctorDetails from "./pages/DoctorDetails";
import DoctorSchedule from "./pages/dashboards/DoctorSchedule";
import DoctorQueue from "./pages/doctor/DoctorQueue";
import Consultation from "./pages/doctor/Consultation";
import AdminDoctors from "./pages/admin/AdminDoctors";
import AdminDoctorSchedule from "./pages/admin/AdminDoctorSchedule";
import AdminDepartments from "./pages/admin/AdminDepartments";
import AdminHolidays from "./pages/admin/AdminHolidays";
import AdminReports from "./pages/admin/AdminReports";
import AdminStaff from "./pages/admin/AdminStaff";
import AdminHospital from "./pages/admin/AdminHospital";
import SuperAdmin from "./pages/SuperAdmin";
import MyAppointments from "./pages/patient/MyAppointments";
import AppointmentDetails from "./pages/patient/AppointmentDetails";
import AppointmentQr from "./pages/patient/AppointmentQr";
import BookingConfirm from "./pages/patient/BookingConfirm";
import Reschedule from "./pages/patient/Reschedule";
import FollowUps from "./pages/patient/FollowUps";
import Notifications from "./pages/Notifications";
import PrescriptionList from "./pages/clinical/PrescriptionList";
import PrescriptionDetailPage from "./pages/clinical/PrescriptionDetailPage";
import ReceptionHome from "./pages/reception/ReceptionHome";
import CheckIn from "./pages/reception/CheckIn";
import ReceptionQueue from "./pages/reception/Queue";
import WalkIn from "./pages/reception/WalkIn";
import ReceptionAppointments from "./pages/reception/Appointments";
import {
  PatientDashboard,
  DoctorDashboard,
  ReceptionDashboard,
  AdminDashboard,
} from "./pages/dashboards/Dashboards";

const guard = (roles, el) => <ProtectedRoute roles={roles}>{el}</ProtectedRoute>;
const HOSPITAL_ADMIN_ONLY = [ROLES.HOSPITAL_ADMIN]; // SUPER_ADMIN has no hospital assignment
const PATIENT_ONLY = [ROLES.PATIENT];
const DOCTOR_ONLY = [ROLES.DOCTOR];
const RECEPTION_ONLY = [ROLES.RECEPTIONIST];

export default function App() {
  return (
    <>
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/verify" element={<Verify />} />
          <Route path="/unauthorized" element={<Unauthorized />} />

          <Route path="/hospitals" element={<HospitalSearch />} />
          <Route path="/hospitals/:hospitalId" element={<HospitalDetails />} />
          <Route path="/doctors" element={<DoctorSearch />} />
          <Route path="/doctors/:doctorId" element={<DoctorDetails />} />

          <Route path="/patient" element={guard(PATIENT_ONLY, <PatientDashboard />)} />
          <Route path="/patient/appointments" element={guard(PATIENT_ONLY, <MyAppointments />)} />
          <Route path="/patient/prescriptions" element={guard(PATIENT_ONLY, <PrescriptionList role="PATIENT" />)} />
          <Route path="/patient/prescriptions/:prescriptionId" element={guard(PATIENT_ONLY, <PrescriptionDetailPage role="PATIENT" />)} />
          <Route path="/patient/follow-ups" element={guard(PATIENT_ONLY, <FollowUps />)} />
          <Route path="/appointments/:appointmentId" element={guard(PATIENT_ONLY, <AppointmentDetails />)} />
          <Route path="/appointments/:appointmentId/qr" element={guard(PATIENT_ONLY, <AppointmentQr />)} />
          <Route path="/appointments/:appointmentId/confirm" element={guard(PATIENT_ONLY, <BookingConfirm />)} />
          <Route path="/appointments/:appointmentId/reschedule" element={guard(PATIENT_ONLY, <Reschedule />)} />

          <Route path="/doctor" element={guard(DOCTOR_ONLY, <DoctorDashboard />)} />
          <Route path="/doctor/queue" element={guard(DOCTOR_ONLY, <DoctorQueue />)} />
          <Route path="/doctor/consultations/:appointmentId" element={guard(DOCTOR_ONLY, <Consultation />)} />
          <Route path="/doctor/prescriptions" element={guard(DOCTOR_ONLY, <PrescriptionList role="DOCTOR" />)} />
          <Route path="/doctor/prescriptions/:prescriptionId" element={guard(DOCTOR_ONLY, <PrescriptionDetailPage role="DOCTOR" />)} />
          <Route path="/doctor/schedule" element={guard(DOCTOR_ONLY, <DoctorSchedule />)} />

          <Route path="/reception" element={guard(RECEPTION_ONLY, <ReceptionHome />)} />
          <Route path="/reception/overview" element={<Navigate to="/reception" replace />} />
          <Route path="/reception/check-in" element={guard(RECEPTION_ONLY, <CheckIn />)} />
          <Route path="/reception/queue" element={guard(RECEPTION_ONLY, <ReceptionQueue />)} />
          <Route path="/reception/walk-in" element={guard(RECEPTION_ONLY, <WalkIn />)} />
          <Route path="/reception/appointments" element={guard(RECEPTION_ONLY, <ReceptionAppointments />)} />

          <Route path="/admin" element={guard(HOSPITAL_ADMIN_ONLY, <AdminDashboard />)} />
          <Route path="/admin/doctors" element={guard(HOSPITAL_ADMIN_ONLY, <AdminDoctors />)} />
          <Route path="/admin/doctors/:doctorId/schedule" element={guard(HOSPITAL_ADMIN_ONLY, <AdminDoctorSchedule />)} />
          <Route path="/admin/departments" element={guard(HOSPITAL_ADMIN_ONLY, <AdminDepartments />)} />
          <Route path="/admin/holidays" element={guard(HOSPITAL_ADMIN_ONLY, <AdminHolidays />)} />
          <Route path="/admin/reports" element={guard(HOSPITAL_ADMIN_ONLY, <AdminReports />)} />
          <Route path="/admin/staff" element={guard(HOSPITAL_ADMIN_ONLY, <AdminStaff />)} />
          <Route path="/admin/hospital" element={guard(HOSPITAL_ADMIN_ONLY, <AdminHospital />)} />
          <Route path="/super-admin" element={guard([ROLES.SUPER_ADMIN], <SuperAdmin />)} />

          <Route path="/notifications" element={<ProtectedRoute><Notifications /></ProtectedRoute>} />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </>
  );
}