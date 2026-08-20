import { AuthForm } from "@/components/auth-form";

type LoginPageProps = {
  searchParams?: Promise<{
    next?: string | string[];
  }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const resolvedSearchParams = searchParams ? await searchParams : {};
  const nextPath = getSafeNextPath(getSingleValue(resolvedSearchParams.next));

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f5f7fa] px-4 py-10">
      <AuthForm mode="login" nextPath={nextPath} />
    </main>
  );
}

function getSingleValue(value?: string | string[]) {
  return Array.isArray(value) ? value[0] : value;
}

function getSafeNextPath(value?: string) {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return "/dashboard";
  }
  return value;
}
