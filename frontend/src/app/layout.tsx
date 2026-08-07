import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Portfolio Tracker",
  description: "Read-only portfolio dashboard",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="tr" className="h-full antialiased">
      <body className="min-h-full">{children}</body>
    </html>
  );
}
