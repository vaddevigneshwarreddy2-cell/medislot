export const ROLES = {
  PATIENT: "PATIENT",
  DOCTOR: "DOCTOR",
  RECEPTIONIST: "RECEPTIONIST",
  HOSPITAL_ADMIN: "HOSPITAL_ADMIN",
  SUPER_ADMIN: "SUPER_ADMIN",
};

export function dashboardPath(role) {
  switch (role) {
    case ROLES.PATIENT: return "/patient";
    case ROLES.DOCTOR: return "/doctor";
    case ROLES.RECEPTIONIST: return "/reception";
    case ROLES.HOSPITAL_ADMIN: return "/admin";
    case ROLES.SUPER_ADMIN: return "/super-admin";
    default: return "/";
  }
}