"use server";

import { redirect } from "next/navigation";

import { login, register } from "@/lib/api";
import { setAuthToken } from "@/lib/auth-cookie";

export type AuthFormState = {
  message: string;
  status: "idle" | "error";
};

export async function loginAction(
  _previousState: AuthFormState,
  formData: FormData,
): Promise<AuthFormState> {
  return authenticate("login", formData);
}

export async function registerAction(
  _previousState: AuthFormState,
  formData: FormData,
): Promise<AuthFormState> {
  return authenticate("register", formData);
}

async function authenticate(mode: "login" | "register", formData: FormData): Promise<AuthFormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  const next = getSafeNextPath(String(formData.get("next") ?? "/dashboard"));

  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return { message: "Gecerli bir email gir.", status: "error" };
  }

  if (password.length < 8 || password.length > 72) {
    return { message: "Sifre 8-72 karakter arasinda olmali.", status: "error" };
  }

  try {
    const response = mode === "login"
      ? await login({ email, password })
      : await register({ email, password });
    await setAuthToken(response.token);
  } catch (error) {
    return {
      message: error instanceof Error ? error.message : "Kimlik dogrulama basarisiz.",
      status: "error",
    };
  }

  redirect(next);
}

function getSafeNextPath(value: string) {
  if (!value.startsWith("/") || value.startsWith("//")) {
    return "/dashboard";
  }
  if (value.startsWith("/login") || value.startsWith("/register")) {
    return "/dashboard";
  }
  return value;
}
