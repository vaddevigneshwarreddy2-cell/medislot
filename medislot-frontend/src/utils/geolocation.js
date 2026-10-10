// Resolves { lat, lng }. Rejects with an Error whose message is safe to show the user.
// The position is used for one search and is never stored.
export function getCurrentLocation() {
  return new Promise((resolve, reject) => {
    if (typeof navigator === "undefined" || !("geolocation" in navigator)) {
      reject(new Error("Your browser does not support location. Search for an area instead."));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      (err) => {
        switch (err.code) {
          case 1:
            reject(new Error("Location permission was denied. Allow location access in your browser, or search for an area instead."));
            break;
          case 2:
            reject(new Error("Your location is unavailable right now. Search for an area instead."));
            break;
          case 3:
            reject(new Error("Finding your location timed out. Try again, or search for an area instead."));
            break;
          default:
            reject(new Error("Unable to determine your location."));
        }
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 }
    );
  });
}