import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import HospitalDetails from "../pages/HospitalDetails";
import { getHospital } from "../api/hospitals";

vi.mock("../api/hospitals", () => ({ getHospital: vi.fn() }));

const base = {
  id: 1, name: "MediCare Hospital", addressLine: "Road 12", city: "Hyderabad", state: "Telangana",
  postalCode: "500034", phone: null, distanceKm: null, availableDoctors: 0,
  departments: [{ id: 1, name: "Cardiology", description: null }],
};

function setup() {
  render(
    <MemoryRouter initialEntries={["/hospitals/1"]}>
      <Routes><Route path="/hospitals/:hospitalId" element={<HospitalDetails />} /></Routes>
    </MemoryRouter>
  );
}

describe("Hospital details", () => {
  it("opens Google Maps directions to the stored coordinates in a new tab", async () => {
    getHospital.mockResolvedValue({ ...base, latitude: "17.4130000", longitude: "78.4490000" });
    setup();
    const link = await screen.findByRole("link", { name: /Get Directions in Google Maps/ });
    expect(link).toHaveAttribute("href",
      "https://www.google.com/maps/dir/?api=1&destination=17.4130000,78.4490000&travelmode=driving");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link.getAttribute("rel")).toContain("noopener");
  });

  it("hides the map buttons when the hospital has no coordinates", async () => {
    getHospital.mockResolvedValue({ ...base, latitude: null, longitude: null });
    setup();
    expect(await screen.findByText(/no map location yet/)).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /Get Directions/ })).not.toBeInTheDocument();
  });

  it("shows the server message when the hospital is missing", async () => {
    getHospital.mockRejectedValue({ response: { data: { code: "HOSPITAL_NOT_FOUND", message: "Hospital not found." } } });
    setup();
    expect(await screen.findByRole("alert")).toHaveTextContent("Hospital not found.");
  });
});