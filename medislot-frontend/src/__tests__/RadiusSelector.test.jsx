import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RadiusSelector from "../components/RadiusSelector";

describe("RadiusSelector", () => {
  it("marks the selected radius and reports clicks", async () => {
    const onChange = vi.fn();
    render(<RadiusSelector options={[1, 3, 5, 10]} value={3} onChange={onChange} />);
    expect(screen.getByRole("button", { name: "3 km" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "5 km" })).toHaveAttribute("aria-pressed", "false");
    await userEvent.click(screen.getByRole("button", { name: "5 km" }));
    expect(onChange).toHaveBeenCalledWith(5);
  });
});