import { Routes, Route } from "react-router-dom";
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
import AdminDoctors from "./pages/admin/AdminDoctors";
import AdminDoctorSchedule from "./pages/admin/AdminDoctorSchedule";
import AdminDepartments from "./pages/admin/AdminDepartments";
import AdminHolidays from "./pages/admin/AdminHolidays";
import {
  PatientDashboard,
  DoctorDashboard,
  ReceptionDashboard,
  AdminDashboard,
} from "./pages/dashboards/Dashboards";

const guard = (roles, el) => <ProtectedRoute roles={roles}>{el}</ProtectedRoute>;
const HOSPITAL_ADMIN_ONLY = [ROLES.HOSPITAL_ADMIN]; // SUPER_ADMIN has no hospital assignment

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

          <Route path="/patient" element={guard([ROLES.PATIENT], <PatientDashboard />)} />
          <Route path="/doctor" element={guard([ROLES.DOCTOR], <DoctorDashboard />)} />
          <Route path="/doctor/schedule" element={guard([ROLES.DOCTOR], <DoctorSchedule />)} />
          <Route path="/reception" element={guard([ROLES.RECEPTIONIST], <ReceptionDashboard />)} />
          <Route path="/admin" element={guard([ROLES.HOSPITAL_ADMIN, ROLES.SUPER_ADMIN], <AdminDashboard />)} />
          <Route path="/admin/doctors" element={guard(HOSPITAL_ADMIN_ONLY, <AdminDoctors />)} />
          <Route path="/admin/doctors/:doctorId/schedule" element={guard(HOSPITAL_ADMIN_ONLY, <AdminDoctorSchedule />)} />
          <Route path="/admin/departments" element={guard(HOSPITAL_ADMIN_ONLY, <AdminDepartments />)} />
          <Route path="/admin/holidays" element={guard(HOSPITAL_ADMIN_ONLY, <AdminHolidays />)} />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </>
  );
}