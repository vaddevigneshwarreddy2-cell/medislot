import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import Login from "../pages/Login";

const auth = vi.hoisted(() => ({ value: { user: null, login: vi.fn() } }));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth.value }));

function setup() {
  render(
    <MemoryRouter initialEntries={["/login"]}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/patient" element={<p>patient home</p>} />
        <Route path="/verify" element={<p>verify page</p>} />
      </Routes>
    </MemoryRouter>
  );
}

describe("Login page", () => {
  beforeEach(() => { auth.value = { user: null, login: vi.fn() }; });

  it("asks for both fields before calling the server", async () => {
    setup();
    await userEvent.click(screen.getByRole("button", { name: "Log in" }));
    expect(screen.getByRole("alert")).toHaveTextContent("Enter your email and password.");
    expect(auth.value.login).not.toHaveBeenCalled();
  });

  it("logs in and goes to the role dashboard", async () => {
    auth.value.login.mockResolvedValue({ role: "PATIENT" });
    setup();
    await userEvent.type(screen.getByLabelText("Email"), "a@b.com");
    await userEvent.type(screen.getByLabelText("Password"), "Secret123");
    await userEvent.click(screen.getByRole("button", { name: "Log in" }));
    expect(auth.value.login).toHaveBeenCalledWith("a@b.com", "Secret123");
    expect(await screen.findByText("patient home")).toBeInTheDocument();
  });

  it("shows the server message for wrong credentials", async () => {
    auth.value.login.mockRejectedValue({ response: { data: { code: "INVALID_CREDENTIALS", message: "Invalid email or password." } } });
    setup();
    await userEvent.type(screen.getByLabelText("Email"), "a@b.com");
    await userEvent.type(screen.getByLabelText("Password"), "wrong");
    await userEvent.click(screen.getByRole("button", { name: "Log in" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid email or password.");
  });

  it("sends an unverified account to the verify page", async () => {
    auth.value.login.mockRejectedValue({ response: { data: { code: "ACCOUNT_NOT_VERIFIED", message: "Verify first." } } });
    setup();
    await userEvent.type(screen.getByLabelText("Email"), "a@b.com");
    await userEvent.type(screen.getByLabelText("Password"), "Secret123");
    await userEvent.click(screen.getByRole("button", { name: "Log in" }));
    expect(await screen.findByText("verify page")).toBeInTheDocument();
  });
});