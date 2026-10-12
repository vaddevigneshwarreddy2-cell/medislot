# MediSlot API

Base path `/api`. JSON in and out. Protected endpoints need `Authorization: Bearer <accessToken>`.

Response envelope: `{ "success": true, "message": "...", "code": null, "data": ..., "timestamp": "..." }`.
Errors: `{ "success": false, "message": "...", "code": "SLOT_UNAVAILABLE" }`. No stack traces are ever returned.

Common statuses: 400 validation, 401 not logged in, 403 wrong role, 404 not found (also used for other users' data),
409 conflict, 410 expired hold, 429 rate limited (`Retry-After` header).

## Public
| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Service and database status |
| GET | `/hospitals/config` | Default radius and radius options |
| GET | `/hospitals/nearby?latitude&longitude&radiusKm` | Hospitals within the radius, nearest first |
| GET | `/hospitals/search?name&latitude&longitude&radiusKm` | Name search, optionally combined with location |
| GET | `/hospitals`, `/hospitals/{id}` | List (paged) and details |
| GET | `/hospitals/{id}/doctors`, `/hospitals/{id}/departments` | Doctors and departments of a hospital |
| GET | `/doctors?name&specialization&hospitalId&departmentId&latitude&longitude&radiusKm` | Doctor search |
| GET | `/doctors/{id}` | Profile and weekly schedule |
| GET | `/doctors/{id}/availability?date=YYYY-MM-DD` | Free slots (booked and held slots removed) |
| GET | `/doctors/{id}/availability/upcoming?days` | Slot count per day |

## Authentication
| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Patient registration |
| POST | `/auth/verify`, `/auth/resend-code` | OTP verification (when enabled) |
| POST | `/auth/login` | Returns access and refresh tokens |
| POST | `/auth/refresh` | Rotates the refresh token (each works once) |
| POST | `/auth/logout` | Revokes the refresh token |
| GET | `/auth/me` | Current user |

## Patient (role PATIENT)
| Method | Path | Purpose |
|---|---|---|
| POST | `/appointments/hold` | Hold a slot for 5 minutes |
| POST | `/appointments` | Confirm a hold |
| GET | `/appointments/my?scope=upcoming|history` | My appointments |
| GET | `/appointments/{id}` | Details including hospital coordinates |
| PUT | `/appointments/{id}/cancel`, `/reschedule`, `/attendance` | Change or confirm attendance |
| GET | `/appointments/{id}/qr`, `/appointments/{id}/queue` | QR code, queue position |
| GET | `/patient/dashboard`, `/patient/follow-ups` | Counts, follow-up dates |
| GET | `/patient/prescriptions`, `/{id}`, `/{id}/pdf` | Prescriptions |
| GET/PUT | `/notifications`, `/notifications/unread-count`, `/notifications/{id}/read`, `/notifications/read-all` | Inbox (any logged-in role) |

## Doctor (role DOCTOR)
| Method | Path | Purpose |
|---|---|---|
| GET/PUT | `/doctor/profile`, `/doctor/availability` | Profile, weekly hours |
| GET/POST/DELETE | `/doctor/leave`, `/doctor/leave/{id}` | Leave days |
| GET | `/doctor/queue`, `/doctor/dashboard` | Today's queue, counts |
| POST | `/doctor/queue/call-next`, `/{id}/call`, `/{id}/start` | Work the queue |
| GET/PUT | `/doctor/consultations/{appointmentId}` | Load / save draft |
| POST | `/doctor/consultations/{appointmentId}/complete` | Complete, lock, create prescription |
| GET | `/doctor/prescriptions`, `/{id}`, `/{id}/pdf` | Prescriptions issued |

## Reception (role RECEPTIONIST)
| Method | Path | Purpose |
|---|---|---|
| GET | `/reception/doctors`, `/appointments`, `/queue`, `/attention` | Lists; attention = overdue, not checked in |
| POST | `/reception/check-in` | Check in by QR code |
| POST | `/reception/appointments/{id}/check-in` | Manual check-in |
| POST | `/reception/walk-ins` | Register and queue a walk-in |
| PUT | `/reception/appointments/{id}/status` | CANCELLED or NO_SHOW only |

## Hospital admin (role HOSPITAL_ADMIN, own hospital only)
| Method | Path | Purpose |
|---|---|---|
| GET/PUT | `/admin/hospital`, `/admin/hospital/location` | Profile, address and coordinates |
| GET/POST/PUT/PATCH | `/admin/doctors`, `/{id}`, `/{id}/active` | Doctors |
| GET/PUT/POST/DELETE | `/admin/doctors/{id}/availability`, `/{id}/leave` | A doctor's schedule and leave |
| GET/POST/PUT/PATCH | `/admin/departments` | Departments |
| GET/POST/DELETE | `/admin/holidays` | Hospital holidays |
| GET/POST/PATCH | `/admin/staff` | Receptionists |
| GET | `/admin/dashboard?period=today|week|month` | Counts |
| GET | `/admin/reports?from&to&granularity`, `/admin/reports/export` | Report, CSV (counts only) |

## Super admin (role SUPER_ADMIN)
| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/super-admin/hospitals` | List all / create |
| PUT/PATCH | `/super-admin/hospitals/{id}`, `/location`, `/active` | Edit, enable/disable |
| GET/POST/PATCH | `/super-admin/hospitals/{id}/admins` | Hospital administrators |
| GET | `/super-admin/audit-logs?action&page&size` | Read-only audit trail |

## Rate limits (per IP)
Login 10/min, register 5/10 min, resend-code 3/10 min, verify 10/10 min, refresh 30/min, everything else 300/min.

## Appointment states
HELD, CONFIRMED, ATTENDANCE_PENDING, CHECKED_IN, WAITING, IN_CONSULTATION, COMPLETED, CANCELLED, NO_SHOW, EXPIRED, RESCHEDULED.
Only the backend state machine changes them; invalid moves return `409 INVALID_STATUS_TRANSITION`.