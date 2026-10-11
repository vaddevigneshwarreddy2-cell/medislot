import { directionsUrl, hasCoordinates, viewLocationUrl } from "../utils/maps";

const hospital = { latitude: "17.4130000", longitude: "78.4490000", name: "MediCare", id: 1 };

describe("maps links", () => {
  it("builds the directions URL from the stored coordinates only", () => {
    const url = directionsUrl(hospital);
    expect(url).toBe("https://www.google.com/maps/dir/?api=1&destination=17.4130000,78.4490000&travelmode=driving");
  });

  it("never puts names, ids or other data in the URL", () => {
    const url = directionsUrl({ ...hospital, patientName: "Asha", appointmentId: 99 });
    expect(url).not.toMatch(/Asha|99|MediCare/);
  });

  it("builds a view-location URL", () => {
    expect(viewLocationUrl(hospital)).toContain("query=17.4130000,78.4490000");
  });

  it("detects missing or invalid coordinates", () => {
    expect(hasCoordinates(hospital)).toBe(true);
    expect(hasCoordinates({ latitude: null, longitude: 78 })).toBe(false);
    expect(hasCoordinates({ latitude: "abc", longitude: 78 })).toBe(false);
    expect(hasCoordinates(null)).toBe(false);
  });
});