// JL Enterprises brand palette (matches the web Tailwind config).
export const palette = {
  navy: "#0b2447",
  navy600: "#19376d",
  brand: "#576cbc",
  brandSky: "#a5d7e8",
  orange: "#f97316",
  orange600: "#ea580c",
  amber: "#fbbf24",
};

export interface Theme {
  dark: boolean;
  bg: string;
  surface: string;
  surfaceAlt: string;
  border: string;
  text: string;
  textMuted: string;
  primary: string;
  accent: string;
  onPrimary: string;
  danger: string;
  success: string;
  warn: string;
}

export const lightTheme: Theme = {
  dark: false,
  bg: "#eef1f6",
  surface: "#ffffff",
  surfaceAlt: "#f8fafc",
  border: "#e2e8f0",
  text: "#0b2447",
  textMuted: "#64748b",
  primary: "#0b2447",
  accent: "#f97316",
  onPrimary: "#ffffff",
  danger: "#dc2626",
  success: "#16a34a",
  warn: "#f59e0b",
};

export const darkTheme: Theme = {
  dark: true,
  bg: "#0b1220",
  surface: "#141c2e",
  surfaceAlt: "#1b2438",
  border: "#26304a",
  text: "#e5eaf3",
  textMuted: "#93a1bd",
  primary: "#576cbc",
  accent: "#f97316",
  onPrimary: "#ffffff",
  danger: "#f87171",
  success: "#4ade80",
  warn: "#fbbf24",
};
