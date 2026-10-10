import { createContext, useContext, useEffect, useState } from "react";
import api from "../api/axios";
import { tokenStorage } from "./tokenStorage";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(tokenStorage.getRefresh()));

  // Restore the session on page load.
  useEffect(() => {
    if (!tokenStorage.getRefresh()) return;
    let cancelled = false;
    api
      .get("/auth/me")
      .then((res) => { if (!cancelled) setUser(res.data.data); })
      .catch(() => { if (!cancelled) setUser(null); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  // The axios interceptor fires this when a refresh is rejected.
  useEffect(() => {
    const onForcedLogout = () => setUser(null);
    window.addEventListener("auth:logout", onForcedLogout);
    return () => window.removeEventListener("auth:logout", onForcedLogout);
  }, []);

  async function login(email, password) {
    const res = await api.post("/auth/login", { email, password });
    const d = res.data.data;
    tokenStorage.set(d.accessToken, d.refreshToken);
    setUser(d.user);
    return d.user;
  }

  async function logout() {
    const refreshToken = tokenStorage.getRefresh();
    try {
      if (refreshToken) await api.post("/auth/logout", { refreshToken });
    } catch {
      // ignore: we clear local state regardless
    }
    tokenStorage.clear();
    setUser(null);
  }

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
