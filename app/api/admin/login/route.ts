import { NextResponse } from "next/server";
import { ADMIN_COOKIE, adminPassword, adminSessionToken, checkAdminPassword } from "@/lib/admin-auth";

export async function POST(req: Request) {
  if (!adminPassword()) {
    return NextResponse.json(
      { error: "Admin login is disabled — set the ADMIN_PASSWORD environment variable." },
      { status: 503 }
    );
  }
  const { password } = await req.json().catch(() => ({ password: "" }));
  if (typeof password !== "string" || !checkAdminPassword(password)) {
    return NextResponse.json({ error: "Incorrect password" }, { status: 401 });
  }
  const res = NextResponse.json({ ok: true });
  res.cookies.set(ADMIN_COOKIE, adminSessionToken()!, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60 * 8, // 8 hours
  });
  return res;
}
