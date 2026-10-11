import { render, screen } from "@testing-library/react";
import StackedBars from "../components/charts/StackedBars";

const data = [
  { label: "2026-10-01", booked: 4, completed: 2, cancelled: 1, noShow: 0 },
  { label: "2026-10-02", booked: 0, completed: 0, cancelled: 0, noShow: 0 },
  { label: "2026-10-03", booked: 3, completed: 3, cancelled: 0, noShow: 0 },
];

describe("StackedBars", () => {
  it("describes the chart for screen readers with the total", () => {
    render(<StackedBars data={data} unit="day" />);
    expect(screen.getByRole("img", { name: /Appointments per day: 7 in total over 3 periods/ })).toBeInTheDocument();
  });

  it("offers the same numbers as a table", () => {
    render(<StackedBars data={data} unit="day" />);
    expect(screen.getByText("Show data as a table")).toBeInTheDocument();
    expect(screen.getAllByText("2026-10-03").length).toBeGreaterThan(0);
  });

  it("copes with an empty range", () => {
    render(<StackedBars data={[]} unit="day" />);
    expect(screen.getByRole("img")).toBeInTheDocument();
  });
});