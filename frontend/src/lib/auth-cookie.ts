import { cookies } from "next/headers";

import { AUTH_COOKIE_MAX_AGE_SECONDS, AUTH_COOKIE_NAME } from "@/lib/auth-constants";

export async function getAuthToken() {
  return (await cookies()).get(AUTH_COOKIE_NAME)?.value ?? null;
}

export async function setAuthToken(token: string) {
  (await cookies()).set({
    httpOnly: true,
    maxAge: AUTH_COOKIE_MAX_AGE_SECONDS,
    name: AUTH_COOKIE_NAME,
    path: "/",
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    value: token,
  });
}

export async function clearAuthToken() {
  (await cookies()).delete(AUTH_COOKIE_NAME);
}
