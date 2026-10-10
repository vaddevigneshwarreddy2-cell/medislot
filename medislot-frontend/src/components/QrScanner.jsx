import { useEffect, useRef, useState } from "react";

export function scannerSupported() {
  return typeof window !== "undefined" && "BarcodeDetector" in window && Boolean(navigator.mediaDevices?.getUserMedia);
}

// Camera scanning with the browser's built-in BarcodeDetector. Calls onCode(text) once, then stops.
export default function QrScanner({ onCode }) {
  const video = useRef(null);
  const handler = useRef(onCode);
  handler.current = onCode;
  const [active, setActive] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!active) return undefined;
    let stream = null;
    let timer = null;
    let stopped = false;

    (async () => {
      try {
        const detector = new window.BarcodeDetector({ formats: ["qr_code"] });
        stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
        if (stopped) { stream.getTracks().forEach((t) => t.stop()); return; }
        video.current.srcObject = stream;
        await video.current.play();
        timer = setInterval(async () => {
          try {
            const found = await detector.detect(video.current);
            if (found.length > 0) {
              clearInterval(timer);
              setActive(false);
              handler.current(found[0].rawValue);
            }
          } catch { /* a frame failed to decode: try again */ }
        }, 400);
      } catch {
        setError("Could not open the camera. Check the browser permission, or type the code instead.");
        setActive(false);
      }
    })();

    return () => {
      stopped = true;
      clearInterval(timer);
      if (stream) stream.getTracks().forEach((t) => t.stop());
    };
  }, [active]);

  return (
    <div>
      {!active ? (
        <button type="button" onClick={() => { setError(null); setActive(true); }}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium hover:bg-slate-100">
          Scan with camera
        </button>
      ) : (
        <div>
          <video ref={video} muted playsInline className="w-full max-w-sm rounded-lg bg-black" />
          <button type="button" onClick={() => setActive(false)}
                  className="mt-2 text-sm font-medium text-brand-600 hover:underline">
            Stop camera
          </button>
        </div>
      )}
      {error && <p role="alert" className="mt-2 text-sm text-red-700">{error}</p>}
    </div>
  );
}