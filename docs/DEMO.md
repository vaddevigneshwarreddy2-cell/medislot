# Demo walkthroughs (demo profile, password `Demo@1234`)

Use two browser windows (one normal, one private).

## Patient
1. Register (or use `patient@demo.com`) and log in.
2. **Find hospitals**: use a location chip such as "Banjara Hills", pick **3 km**, then try **1 km** to see the "try 5/10/25 km" prompt.
3. Open **MediCare Hospital**: **View Location** and **Get Directions in Google Maps**.
4. Open **Dr. Ravi Kumar**, pick a weekday slot, **Hold this slot** (5-minute countdown), **Confirm**.
5. On the appointment page: **View QR Code**, **Get Directions**, reschedule, cancel.

## Receptionist (`reception@demo.com`)
1. **QR check-in**: paste the patient's code. The patient page shows the queue number within seconds.
2. **Walk-in**: register a patient at the desk. **Appointments**: search, check in, no-show.

## Doctor (`dr.arjun@demo.com` or `dr.meera@demo.com`)
1. **Today's queue**: Call next, then **Start consultation**.
2. Notes, diagnosis, medicines, follow-up date, **Complete**.

## Patient again
**Prescriptions**: view and download the PDF. **Follow-ups**: book a return visit. The notification bell shows updates.

## Hospital admin (`admin@demo.com`)
Departments, doctors, receptionists, holidays, hospital location, dashboard, **Reports** (charts and CSV).

## Super admin (`super@demo.com`)
Hospitals, hospital admins, and the audit log API.

## Test the race
Run `mvn "-Dtest=SlotConcurrencyIT" test`: 10 simultaneous holds on one slot, exactly one succeeds.