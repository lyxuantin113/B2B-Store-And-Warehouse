"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/context/auth-context";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  LayoutDashboard,
  Users,
  ShoppingBag,
  PackageCheck,
  Building,
  Boxes,
  MessageSquare,
  Settings,
  ShieldAlert,
  LogOut,
  ChevronRight,
  Menu,
  X,
  ExternalLink,
  Store,
} from "lucide-react";

interface AdminNavItem {
  href: string;
  label: string;
  icon: React.ReactNode;
  badge?: string;
}

const navItems: AdminNavItem[] = [
  { href: "/admin", label: "Tổng Quan & Báo Cáo", icon: <LayoutDashboard className="w-4 h-4" /> },
  { href: "/admin/partners", label: "Đối Tác B2B CRM", icon: <Users className="w-4 h-4" />, badge: "CRM" },
  { href: "/admin/orders", label: "Xử Lý Đơn Hàng", icon: <PackageCheck className="w-4 h-4" /> },
  { href: "/admin/inventory", label: "Tồn Kho Đa Kho", icon: <Boxes className="w-4 h-4" /> },
  { href: "/admin/branches", label: "Kho & Chi Nhánh", icon: <Building className="w-4 h-4" /> },
  { href: "/admin/catalog", label: "Danh Mục & Giá Sỉ", icon: <ShoppingBag className="w-4 h-4" /> },
  { href: "/admin/chat", label: "Live Chat CSKH", icon: <MessageSquare className="w-4 h-4" />, badge: "Realtime" },
  { href: "/admin/settings", label: "Cấu Hình & Phân Quyền", icon: <Settings className="w-4 h-4" /> },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, isStaff, loading, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    if (!loading && !user) {
      router.push(`/login?redirect=${encodeURIComponent(pathname)}`);
    }
  }, [loading, user, pathname, router]);

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
        <Skeleton className="h-16 w-full rounded-2xl" />
        <Skeleton className="h-96 w-full rounded-2xl" />
      </div>
    );
  }

  if (!user || !isStaff) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center p-4">
        <div className="max-w-md w-full p-8 rounded-3xl border border-rose-500/30 bg-rose-950/20 text-center space-y-4">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-rose-500/20 text-rose-400 flex items-center justify-center">
            <ShieldAlert className="w-7 h-7" />
          </div>
          <h2 className="text-xl font-bold text-white">Truy Cập Bị Từ Chối</h2>
          <p className="text-xs text-slate-300 leading-relaxed">
            Khu vực này chỉ dành riêng cho Quản trị viên, Quản lý kho và Nhân viên bán hàng (ADMIN / MANAGER / SELLER).
          </p>
          <div className="pt-2 flex justify-center gap-3">
            <Link href="/portal">
              <Button variant="primary" size="sm" className="text-xs">
                Về Cổng Đối Tác (B2B Portal)
              </Button>
            </Link>
            <Button variant="outline" size="sm" onClick={() => logout()} className="text-xs">
              Đăng xuất
            </Button>
          </div>
        </div>
      </div>
    );
  }

  const roleBadgeVariant = user.roleCode === "ADMIN" ? "danger" : user.roleCode === "MANAGER" ? "warning" : "info";

  return (
    <div className="min-h-[calc(100vh-4rem)] flex flex-col md:flex-row bg-slate-950">
      {/* Mobile Menu Toggle Bar */}
      <div className="md:hidden flex items-center justify-between p-4 bg-slate-900 border-b border-slate-800">
        <div className="flex items-center gap-2">
          <span className="font-black text-sm text-white">Admin Backoffice</span>
          <Badge variant={roleBadgeVariant} size="sm">
            {user.roleCode}
          </Badge>
        </div>
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="p-2 rounded-xl bg-slate-800 text-slate-300 hover:text-white"
        >
          {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
        </button>
      </div>

      {/* Sidebar Navigation */}
      <aside
        className={`w-full md:w-64 bg-slate-900/90 border-r border-slate-800 p-4 space-y-6 shrink-0 md:block ${
          mobileMenuOpen ? "block" : "hidden"
        }`}
      >
        {/* Staff Identity Card */}
        <div className="p-3.5 rounded-2xl bg-slate-950/80 border border-slate-800 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Nhân sự phụ trách
            </span>
            <Badge variant={roleBadgeVariant} size="sm">
              {user.roleCode}
            </Badge>
          </div>
          <div>
            <div className="text-xs font-bold text-white truncate">{user.fullName}</div>
            <div className="text-[11px] text-slate-400 truncate">{user.email}</div>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="space-y-1">
          <div className="px-3 pb-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
            Phân Hệ Quản Trị
          </div>
          {navItems.map((item) => {
            const isActive = pathname === item.href || (item.href !== "/admin" && pathname.startsWith(item.href));
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setMobileMenuOpen(false)}
                className={`flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all ${
                  isActive
                    ? "bg-brand-600 text-white shadow-md shadow-brand-600/20"
                    : "text-slate-400 hover:text-white hover:bg-slate-800/60"
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className={isActive ? "text-white" : "text-slate-400"}>{item.icon}</span>
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span
                    className={`text-[9px] px-1.5 py-0.5 rounded-md font-bold uppercase ${
                      isActive ? "bg-white/20 text-white" : "bg-slate-800 text-slate-300"
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </Link>
            );
          })}
        </nav>

        {/* Quick Links */}
        <div className="pt-4 border-t border-slate-800 space-y-1">
          <div className="px-3 pb-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
            Liên Kết Ngoài
          </div>
          <Link
            href="/portal"
            target="_blank"
            className="flex items-center justify-between px-3.5 py-2 rounded-xl text-xs text-slate-400 hover:text-white hover:bg-slate-800/40 transition-colors"
          >
            <span className="flex items-center gap-2">
              <Store className="w-3.5 h-3.5" />
              Xem B2B Portal
            </span>
            <ExternalLink className="w-3 h-3 text-slate-400" />
          </Link>
          <button
            onClick={() => logout()}
            className="w-full flex items-center justify-between px-3.5 py-2 rounded-xl text-xs text-rose-400 hover:bg-rose-500/10 transition-colors"
          >
            <span className="flex items-center gap-2">
              <LogOut className="w-3.5 h-3.5" />
              Đăng xuất
            </span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 p-4 sm:p-6 lg:p-8 min-w-0 overflow-y-auto">
        {children}
      </main>
    </div>
  );
}
