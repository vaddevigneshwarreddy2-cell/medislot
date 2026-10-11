import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import Navbar from "../components/Navbar";
import ProtectedRoute from "../auth/ProtectedRoute";

const auth = vi.hoisted(() => ({ value: { user: null, loading: false, logout: vi.fn() } }));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth.value }));
vi.mock("../components/NotificationBell", () => ({ default: () => <span>bell</span> }));

const as = (role) => ({ user: { fullName: "Test User", role }, loading: false, logout: vi.fn() });

describe("Navbar by role", () => {
  it("shows login and register to visitors", () => {
    auth.value = { user: null, loading: false, logout: vi.fn() };
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    expect(screen.getByRole("link", { name: "Log in" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Register" })).toBeInTheDocument();
    expect(screen.queryByText("bell")).not.toBeInTheDocument();
  });

  it("shows patient links and the bell to patients", () => {
    auth.value = as("PATIENT");
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    expect(screen.getByRole("link", { name: "My appointments" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Prescriptions" })).toBeInTheDocument();
    expect(screen.getByText("bell")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Log out" })).toBeInTheDocument();
  });

  it("hides patient links from doctors and points the dashboard link to /doctor", () => {
    auth.value = as("DOCTOR");
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    expect(screen.queryByRole("link", { name: "My appointments" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Dashboard" })).toHaveAttribute("href", "/doctor");
  });
});

describe("ProtectedRoute", () => {
  function setup(roles) {
    render(
      <MemoryRouter initialEntries={["/secret"]}>
        <Routes>
          <Route path="/secret" element={<ProtectedRoute roles={roles}><p>secret page</p></ProtectedRoute>} />
          <Route path="/login" element={<p>login page</p>} />
          <Route path="/unauthorized" element={<p>access denied</p>} />
        </Routes>
      </MemoryRouter>
    );
  }

  it("sends visitors to login", () => {
    auth.value = { user: null, loading: false };
    setup(["DOCTOR"]);
    expect(screen.getByText("login page")).toBeInTheDocument();
  });

  it("refuses the wrong role", () => {
    auth.value = as("PATIENT");
    setup(["DOCTOR"]);
    expect(screen.getByText("access denied")).toBeInTheDocument();
  });

  it("lets the right role in", () => {
    auth.value = as("DOCTOR");
    setup(["DOCTOR"]);
    expect(screen.getByText("secret page")).toBeInTheDocument();
  });

  it("waits while the session is restoring", () => {
    auth.value = { user: null, loading: true };
    setup(["DOCTOR"]);
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });
});