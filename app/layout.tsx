import type { Metadata, Viewport } from "next";
import "./globals.css";
import PWARegister from "@/components/PWARegister";

export const metadata: Metadata = {
  title: "JL Enterprises — Home Appliances & Furniture | Thoothukudi",
  description:
    "Buy ACs, TVs, Refrigerators, Washing Machines & Furniture online. Free door delivery across Tamil Nadu, easy EMI, COD & doorstep service.",
  manifest: "/manifest.webmanifest",
  icons: { icon: "/icon.svg", apple: "/icon.svg" },
  appleWebApp: { capable: true, title: "JL Store", statusBarStyle: "black-translucent" },
};

export const viewport: Viewport = {
  themeColor: "#0b2447",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        {children}
        <PWARegister />
      </body>
    </html>
  );
}
