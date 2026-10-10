/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        brand: { 50: "#eff8ff", 100: "#dbeefe", 500: "#0e7fd9", 600: "#0a68b8", 700: "#0a5496" },
      },
    },
  },
  plugins: [],
};