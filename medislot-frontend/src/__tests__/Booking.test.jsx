import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import DoctorDetails from "../pages/DoctorDetails";
import { getDoctor } from "../api/doctors";
import { holdSlot } from "../api/appointments";

const auth = vi.hoisted(() => ({ value: { user: null } }));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth.value }));
vi.mock("../api/doctors", () => ({ getDoctor: vi.fn() }));
vi.mock("../api/appointments", () => ({ holdSlot: vi.fn() }));
vi.mock("../components/SlotPicker", () => ({
  default: ({ onSelect }) => <button onClick={() => onSelect({ date: "2026-10-14", time: "10:00" })}>pick slot</button>,
}));

const doctor = {
  id: 5, fullName: "Dr. Ravi Kumar", specialization: "Cardiologist", qualification: "MD", experienceYears: 15,
  consultationDurationMin: 30, bio: null, hospitalId: 1, hospitalName: "MediCare Hospital",
  hospitalCity: "Hyderabad", departmentId: 3, departmentName: "Cardiology", schedule: [],
};

function setup() {
  render(
    <MemoryRouter initialEntries={["/doctors/5"]}>
      <Routes>
        <Route path="/doctors/:doctorId" element={<DoctorDetails />} />
        <Route path="/appointments/:id/confirm" element={<p>confirm page</p>} />
        <Route path="/login" element={<p>login page</p>} />
      </Routes>
    </MemoryRouter>
  );
}

describe("Booking from the doctor page", () => {
  beforeEach(() => {
    getDoctor.mockResolvedValue(doctor);
    holdSlot.mockReset();
    auth.value = { user: { role: "PATIENT", fullName: "Asha" } };
  });

  it("keeps the button disabled until a slot is chosen, then holds it and moves to confirmation", async () => {
    holdSlot.mockResolvedValue({ appointmentId: 77 });
    setup();
    const button = await screen.findByRole("button", { name: "Hold this slot and continue" });
    expect(button).toBeDisabled();
    await userEvent.click(screen.getByText("pick slot"));
    expect(button).toBeEnabled();
    await userEvent.click(button);
    expect(holdSlot).toHaveBeenCalledWith("5", "2026-10-14", "10:00");
    expect(await screen.findByText("confirm page")).toBeInTheDocument();
  });

  it("shows the message and clears the choice when the slot was taken", async () => {
    holdSlot.mockRejectedValue({ response: { data: { code: "SLOT_UNAVAILABLE", message: "The selected appointment slot is no longer available." } } });
    setup();
    await userEvent.click(await screen.findByText("pick slot"));
    await userEvent.click(screen.getByRole("button", { name: "Hold this slot and continue" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("no longer available");
    expect(screen.getByRole("button", { name: "Hold this slot and continue" })).toBeDisabled();
  });

  it("sends a visitor to log in instead of booking", async () => {
    auth.value = { user: null };
    setup();
    await userEvent.click(await screen.findByText("pick slot"));
    await userEvent.click(screen.getByRole("button", { name: "Log in to book" }));
    expect(holdSlot).not.toHaveBeenCalled();
    expect(await screen.findByText("login page")).toBeInTheDocument();
  });

  it("does not let staff book", async () => {
    auth.value = { user: { role: "DOCTOR", fullName: "Dr X" } };
    setup();
    expect(await screen.findByText(/Only patient accounts can book/)).toBeInTheDocument();
  });
});