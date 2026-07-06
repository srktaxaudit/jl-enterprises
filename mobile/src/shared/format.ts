/** ₹ formatting to match the web (Indian grouping, no decimals). */
export const inr = (n: number | string | null | undefined): string =>
  "₹" + Math.round(Number(n || 0)).toLocaleString("en-IN");

export const dateTime = (iso?: string | null): string => {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" });
  } catch {
    return "—";
  }
};

export const dateOnly = (iso?: string | null): string => {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleDateString("en-IN", { dateStyle: "medium" });
  } catch {
    return "—";
  }
};
