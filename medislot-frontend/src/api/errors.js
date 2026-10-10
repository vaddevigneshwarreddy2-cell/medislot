export function getApiError(err) {
  const data = err?.response?.data;
  if (data && data.message) {
    return { code: data.code || "ERROR", message: data.message };
  }
  if (err?.response) {
    return { code: "ERROR", message: "Something went wrong. Please try again." };
  }
  return { code: "NETWORK", message: "Cannot reach the MediSlot API. Please try again." };
}
