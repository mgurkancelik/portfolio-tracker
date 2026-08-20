"use client";

import Link from "next/link";
import { useActionState } from "react";
import { useFormStatus } from "react-dom";

import { loginAction, registerAction, type AuthFormState } from "@/lib/auth-actions";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";

const initialState: AuthFormState = {
  message: "",
  status: "idle",
};

type AuthFormProps = {
  mode: "login" | "register";
  nextPath?: string;
};

export function AuthForm({ mode, nextPath = "/dashboard" }: AuthFormProps) {
  const isLogin = mode === "login";
  const [state, formAction] = useActionState(isLogin ? loginAction : registerAction, initialState);

  return (
    <Card className="w-full max-w-md">
      <CardHeader className="space-y-2">
        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
          Portfolio Tracker
        </p>
        <CardTitle className="text-2xl font-semibold text-slate-950 dark:text-zinc-50">
          {isLogin ? "Giriş Yap" : "Kayıt Ol"}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form action={formAction} className="space-y-5">
          <input name="next" type="hidden" value={nextPath} />

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium uppercase tracking-[0.12em] text-slate-500">
              Email
            </span>
            <Input
              autoComplete="email"
              name="email"
              placeholder="ornek@email.com"
              required
              type="email"
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium uppercase tracking-[0.12em] text-slate-500">
              Şifre
            </span>
            <Input
              autoComplete={isLogin ? "current-password" : "new-password"}
              maxLength={72}
              minLength={8}
              name="password"
              placeholder="En az 8 karakter"
              required
              type="password"
            />
          </label>

          {state.status === "error" ? (
            <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
              {state.message}
            </div>
          ) : null}

          <SubmitButton label={isLogin ? "Giriş Yap" : "Kayıt Ol"} />
        </form>

        <p className="mt-5 text-center text-sm text-slate-500">
          {isLogin ? "Hesabın yok mu?" : "Zaten hesabın var mı?"}{" "}
          <Link
            className="font-semibold text-[#1f4f82] underline-offset-4 hover:underline"
            href={isLogin ? "/register" : "/login"}
          >
            {isLogin ? "Kayıt ol" : "Giriş yap"}
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}

function SubmitButton({ label }: { label: string }) {
  const { pending } = useFormStatus();

  return (
    <Button className="w-full" disabled={pending} type="submit">
      {pending ? "İşleniyor..." : label}
    </Button>
  );
}
