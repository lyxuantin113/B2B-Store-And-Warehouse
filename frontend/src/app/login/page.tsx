"use client";

import React, { useState, useEffect, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/context/auth-context";
import { useToast } from "@/context/toast-context";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Lock, Mail, Building2, ArrowRight } from "lucide-react";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const { success, error, info } = useToast();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (searchParams.get("expired")) {
      info("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    }
  }, [searchParams, info]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      error("Vui lòng điền đầy đủ email và mật khẩu.");
      return;
    }

    setLoading(true);
    try {
      const auth = await login(email.trim(), password);
      success(`Đăng nhập thành công! Chào mừng ${auth.fullName}.`);

      if (auth.role === "ADMIN" || auth.role === "MANAGER" || auth.role === "SELLER") {
        router.push("/admin");
      } else if (auth.role === "B2B_PARTNER") {
        if (auth.accountStatus === "ACTIVE") {
          router.push("/portal");
        } else {
          router.push("/onboarding");
        }
      } else {
        router.push("/");
      }
    } catch (err: any) {
      console.error("Login failed", err);
      error(err.message || "Tài khoản hoặc mật khẩu không chính xác.");
    } finally {
      setLoading(false);
    }
  };

  const handleFillDemo = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword("Admin@123456");
  };

  return (
    <Card className="p-6 sm:p-8 space-y-5 border-slate-800 bg-slate-900/80 backdrop-blur-md">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Email đăng nhập"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="ten.congty@b2b.vn"
          required
          leftIcon={<Mail className="w-4 h-4 text-slate-400" />}
          autoComplete="username"
        />

        <Input
          label="Mật khẩu"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          required
          leftIcon={<Lock className="w-4 h-4 text-slate-400" />}
          autoComplete="current-password"
        />

        <Button
          type="submit"
          variant="primary"
          size="lg"
          loading={loading}
          className="w-full mt-2 font-bold shadow-md shadow-brand-600/30"
        >
          <span>Đăng nhập hệ thống</span>
          <ArrowRight className="w-4 h-4 ml-1" />
        </Button>
      </form>

      {/* Quick Demo Credentials */}
      <div className="pt-4 border-t border-slate-800/80 space-y-2">
        <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
          Tài khoản mẫu dùng thử:
        </span>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => handleFillDemo("admin@warehouse.com")}
            className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-[11px] text-brand-300 font-medium transition-colors"
          >
            Admin (Quản trị)
          </button>
          <button
            type="button"
            onClick={() => handleFillDemo("partner@vinmart.vn")}
            className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-[11px] text-emerald-300 font-medium transition-colors"
          >
            Partner (Active B2B)
          </button>
        </div>
      </div>
    </Card>
  );
}

export default function LoginPage() {
  return (
    <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center p-4 py-12">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-brand-600 text-white font-black text-xl shadow-lg shadow-brand-500/25 mx-auto">
            B2B
          </div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            Đăng Nhập Cổng Doanh Nghiệp
          </h1>
          <p className="text-xs text-slate-400">
            Hệ sinh thái phân phối sỉ UMEGO Vietnam
          </p>
        </div>

        <Suspense fallback={<div className="p-8 text-center text-xs text-slate-500">Đang tải biểu mẫu...</div>}>
          <LoginForm />
        </Suspense>

        <div className="text-center text-xs text-slate-400">
          Chưa có tài khoản doanh nghiệp?{" "}
          <Link
            href="/register"
            className="font-bold text-brand-400 hover:text-brand-300 underline underline-offset-4"
          >
            Đăng ký đối tác B2B mới
          </Link>
        </div>
      </div>
    </div>
  );
}
