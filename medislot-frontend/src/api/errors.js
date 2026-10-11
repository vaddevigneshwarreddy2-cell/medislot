export function getApiError(err) {
  const data = err?.response?.data;
  if (data && typeof data === "object" && data.message) {
    return { code: data.code || "ERROR", message: data.message };
  }
  if (err?.response?.status === 429) {
    return { code: "RATE_LIMITED", message: "Too many requests. Please wait a moment and try again." };
  }
  if (err?.response) {
    return { code: "ERROR", message: "Something went wrong. Please try again." };
  }
  return { code: "NETWORK", message: "Cannot reach the MediSlot API. Please try again." };
}