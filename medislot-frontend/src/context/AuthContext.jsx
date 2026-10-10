import { createContext, useCallback, useContext, useEffect, useState } from "react";
import api, { tokenStore } from "../api/axios";

const AuthContext = createContext(null);

export const ROLE_HOME = {
  PATIENT: "/patient",
  DOCTOR: "/doctor",
  RECEPTIONIST: "/reception",
  HOSPITAL_ADMIN: "/admin",
  SUPER_ADMIN: "/super-admin",
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadUser = useCallback(async () => {
    const res = await api.get("/auth/me");
    setUser(res.data.data);
    return res.data.data;
  }, []);

  // On first load, restore the session if a token exists.
  useEffect(() => {
    (async () => {
      if (tokenStore.getAccess()) {
        try {
          await loadUser();
        } catch {
          tokenStore.clear();
        }
      }
      setLoading(false);
    })();
  }, [loadUser]);

  // The axios interceptor fires this when refresh fails.
  useEffect(() => {
    const onLogout = () => setUser(null);
    window.addEventListener("auth:logout", onLogout);
    return () => window.removeEventListener("auth:logout", onLogout);
  }, []);

  const login = useCallback(
    async (email, password) => {
      const res = await api.post("/auth/login", { email, password });
      const { accessToken, refreshToken } = res.data.data;
      tokenStore.set(accessToken, refreshToken);
      return loadUser();
    },
    [loadUser]
  );

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}