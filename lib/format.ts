/** Format a number as Indian Rupees, e.g. 34990 -> "₹34,990" */
export function inr(n: number): string {
  return "₹" + Math.round(n).toLocaleString("en-IN");
}

/** Percentage off given mrp and price */
export function pctOff(mrp: number, price: number): number {
  if (!mrp || mrp <= price) return 0;
  return Math.round(((mrp - price) / mrp) * 100);
}
