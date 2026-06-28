import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "JL Enterprises — Home Appliances & Furniture",
    short_name: "JL Store",
    description:
      "Shop ACs, TVs, Fridges, Washing Machines & Furniture. Free Tamil Nadu delivery, EMI, COD & doorstep service.",
    start_url: "/",
    scope: "/",
    display: "standalone",
    orientation: "portrait",
    background_color: "#0b2447",
    theme_color: "#0b2447",
    icons: [
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" },
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "maskable" },
    ],
    categories: ["shopping", "business"],
  };
}
