import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import HospitalSearch from "../pages/HospitalSearch";
import { searchHospitals } from "../api/hospitals";

vi.mock("../api/hospitals", () => ({
  FALLBACK_CONFIG: { defaultRadiusKm: 3, radiusOptionsKm: [1, 3, 5, 10, 25, 50] },
  getSearchConfig: vi.fn(() => Promise.resolve({ defaultRadiusKm: 3, radiusOptionsKm: [1, 3, 5, 10, 25, 50] })),
  searchHospitals: vi.fn(),
}));
vi.mock("../components/LocationPicker", () => ({
  default: ({ onChange }) => (
    <button onClick={() => onChange({ lat: 17.41, lng: 78.44, label: "Banjara Hills" })}>set location</button>
  ),
}));

const hospital = {
  id: 1, name: "MediCare Hospital", addressLine: "Road 12", city: "Hyderabad", state: "Telangana",
  latitude: "17.4130000", longitude: "78.4490000", phone: null, distanceKm: 4.2,
  availableDoctors: 2, departments: ["Cardiology"],
};

describe("Hospital search", () => {
  beforeEach(() => {
    searchHospitals.mockReset();
    searchHospitals.mockImplementation(async ({ radiusKm }) =>
      radiusKm >= 5
        ? { radiusKm, count: 1, hospitals: [hospital] }
        : { radiusKm, count: 0, hospitals: [] });
  });

  const setup = () => render(<MemoryRouter><HospitalSearch /></MemoryRouter>);

  it("starts empty and does not search until there is a location or a name", () => {
    setup();
    expect(screen.getByText(/Choose a location or enter a hospital name/)).toBeInTheDocument();
    expect(searchHospitals).not.toHaveBeenCalled();
  });

  it("searches with the configured default radius of 3 km", async () => {
    setup();
    await userEvent.click(screen.getByText("set location"));
    await waitFor(() => expect(searchHospitals).toHaveBeenCalled());
    expect(searchHospitals.mock.calls[0][0]).toMatchObject({ radiusKm: 3, origin: { lat: 17.41, lng: 78.44 } });
  });

  it("offers larger radii when nothing is found, and searches again on click", async () => {
    setup();
    await userEvent.click(screen.getByText("set location"));
    expect(await screen.findByText(/No hospitals found within 3 km/)).toBeInTheDocument();
    const fiveKm = screen.getAllByRole("button", { name: "5 km" });
    await userEvent.click(fiveKm[fiveKm.length - 1]); // the suggestion under the message
    expect(await screen.findByText(/MediCare Hospital/)).toBeInTheDocument();
    expect(searchHospitals.mock.calls.at(-1)[0]).toMatchObject({ radiusKm: 5 });
    expect(screen.getByText(/1 hospital found within 5 km/)).toBeInTheDocument();
  });

  it("searches by name alone", async () => {
    setup();
    await userEvent.type(screen.getByRole("textbox", { name: "Search hospitals by name" }), "care{enter}");
    await waitFor(() => expect(searchHospitals).toHaveBeenCalled());
    expect(searchHospitals.mock.calls[0][0]).toMatchObject({ name: "care", origin: null });
  });
});