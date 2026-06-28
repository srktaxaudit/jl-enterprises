import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        navy: { DEFAULT: "#0b2447", 600: "#19376d" },
        brand: { DEFAULT: "#576cbc", sky: "#a5d7e8" },
        orange: { DEFAULT: "#f97316", 600: "#ea580c" },
        amber: { DEFAULT: "#fbbf24" },
      },
      boxShadow: {
        card: "0 12px 30px rgba(15,23,42,.12)",
      },
    },
  },
  plugins: [],
};

export default config;
