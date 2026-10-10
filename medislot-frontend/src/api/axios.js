import axios from "axios";
import { tokenStorage } from "../auth/tokenStorage";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const headers = { "Content-Type": "application/json" };

const api = axios.create({ baseURL, timeout: 15000, headers });
// Plain instance with no interceptors, used only for refreshing (avoids loops).
const bare = axios.create({ baseURL, timeout: 15000, headers });

const AUTH_PATHS = ["/auth/login", "/auth/register", "/auth/verify",
                    "/auth/resend-code", "/auth/refresh", "/auth/logout"];

api.interceptors.request.use((config) => {
  const token = tokenStorage.getAccess();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Refresh tokens are single-use, so concurrent 401s must share ONE refresh call.
let refreshPromise = null;
function refreshTokens() {
  if (!refreshPromise) {
    const refreshToken = tokenStorage.getRefresh();
    refreshPromise = bare
      .post("/auth/refresh", { refreshToken })
      .then((res) => {
        const d = res.data.data;
        tokenStorage.set(d.accessToken, d.refreshToken);
        return d.accessToken;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const isAuthCall = original?.url && AUTH_PATHS.some((p) => original.url.startsWith(p));

    if (status === 401 && original && !original._retry && !isAuthCall && tokenStorage.getRefresh()) {
      original._retry = true;
      try {
        const newToken = await refreshTokens();
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      } catch (refreshErr) {
        // Only end the session if the server rejected the refresh (not on a network blip).
        if (refreshErr.response) {
          tokenStorage.clear();
          window.dispatchEvent(new Event("auth:logout"));
        }
        return Promise.reject(error);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
