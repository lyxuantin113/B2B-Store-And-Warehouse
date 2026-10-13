import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/context/auth-context";
import { ToastProvider } from "@/context/toast-context";
import { Navbar } from "@/components/layout/navbar";
import { Footer } from "@/components/layout/footer";
import { ChatWidget } from "@/components/chat/chat-widget";

export const metadata: Metadata = {
  title: "UMEGO Vietnam — B2B Wholesale & Sample Warehouse Portal",
  description:
    "Nền tảng phân phối sỉ thực phẩm & mẫu thử chuyên biệt dành cho doanh nghiệp B2B tại Việt Nam.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi" className="dark">
      <body className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans">
        <AuthProvider>
          <ToastProvider>
            <Navbar />
            <main className="flex-1">{children}</main>
            <Footer />
            <ChatWidget />
          </ToastProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
