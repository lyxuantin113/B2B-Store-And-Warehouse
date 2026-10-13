"use client";

import React from "react";
import Link from "next/link";
import { useAuth } from "@/context/auth-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Store,
  FileText,
  User,
  LogOut,
  LayoutDashboard,
  ShieldAlert,
  Building2,
} from "lucide-react";

export function Navbar() {
  const { user, isAuthenticated, isStaff, isPartner, logout } = useAuth();

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-800 bg-slate-950/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto flex h-16 items-center justify-between px-4 sm:px-6 lg:px-8">
        {/* Brand Logo */}
        <div className="flex items-center gap-8">
          <Link href="/" className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-brand-600 flex items-center justify-center font-black text-white text-lg shadow-md shadow-brand-500/20">
              B2B
            </div>
            <div>
              <span className="font-extrabold text-base tracking-tight text-white">
                UMEGO <span className="text-brand-500">VIETNAM</span>
              </span>
              <span className="block text-[10px] text-slate-400 font-medium uppercase tracking-widest">
                Wholesale & Warehouse
              </span>
            </div>
          </Link>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-6 text-sm font-medium">
            <Link
              href="/stores"
              className="flex items-center gap-1.5 text-slate-300 hover:text-white transition-colors"
            >
              <Store className="w-4 h-4 text-brand-400" />
              <span>Tìm đại lý & Chi nhánh</span>
            </Link>
            <Link
              href="/policy"
              className="flex items-center gap-1.5 text-slate-300 hover:text-white transition-colors"
            >
              <FileText className="w-4 h-4 text-slate-400" />
              <span>Chính sách B2B</span>
            </Link>
          </nav>
        </div>

        {/* Right Action / Auth Area */}
        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              {/* Partner Dashboard link */}
              {isPartner && (
                <Link href="/portal">
                  <Button variant="outline" size="sm" className="gap-2">
                    <Building2 className="w-4 h-4 text-emerald-400" />
                    <span>Cổng B2B</span>
                    <Badge variant="brand" size="sm">
                      {user?.accountStatus}
                    </Badge>
                  </Button>
                </Link>
              )}

              {/* Admin Portal link */}
              {isStaff && (
                <Link href="/admin">
                  <Button variant="outline" size="sm" className="gap-2">
                    <LayoutDashboard className="w-4 h-4 text-brand-400" />
                    <span>Quản trị ({user?.roleCode})</span>
                  </Button>
                </Link>
              )}

              {/* User menu & Logout */}
              <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-xs text-slate-300">
                <User className="w-3.5 h-3.5 text-slate-400" />
                <span className="font-semibold text-white">{user?.fullName}</span>
              </div>

              <Button
                variant="ghost"
                size="sm"
                onClick={() => logout()}
                title="Đăng xuất"
                className="text-slate-400 hover:text-rose-400"
              >
                <LogOut className="w-4 h-4" />
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2.5">
              <Link href="/login">
                <Button variant="ghost" size="sm">
                  Đăng nhập
                </Button>
              </Link>
              <Link href="/register">
                <Button variant="primary" size="sm">
                  Đăng ký B2B
                </Button>
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
