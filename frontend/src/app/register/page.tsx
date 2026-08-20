import { AuthForm } from "@/components/auth-form";

export default function RegisterPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f5f7fa] px-4 py-10">
      <AuthForm mode="register" />
    </main>
  );
}
