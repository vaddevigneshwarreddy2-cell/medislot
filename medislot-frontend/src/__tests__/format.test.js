import { mmss, to12h } from "../utils/format";
import { getApiError } from "../api/errors";

describe("format helpers", () => {
  it("converts 24-hour times", () => {
    expect(to12h("00:30")).toBe("12:30 AM");
    expect(to12h("13:05")).toBe("1:05 PM");
    expect(to12h("12:00")).toBe("12:00 PM");
  });

  it("formats a countdown and never goes negative", () => {
    expect(mmss(299000)).toBe("04:59");
    expect(mmss(-500)).toBe("00:00");
  });
});

describe("getApiError", () => {
  it("uses the server message when present", () => {
    const e = getApiError({ response: { status: 409, data: { code: "SLOT_UNAVAILABLE", message: "Gone." } } });
    expect(e).toEqual({ code: "SLOT_UNAVAILABLE", message: "Gone." });
  });

  it("gives a friendly message for a rate limit without a body", () => {
    const e = getApiError({ response: { status: 429, data: "" } });
    expect(e.code).toBe("RATE_LIMITED");
    expect(e.message).toMatch(/Too many requests/);
  });

  it("reports a network failure", () => {
    expect(getApiError(new Error("x")).code).toBe("NETWORK");
  });
});