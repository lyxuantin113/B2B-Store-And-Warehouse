"use client";

import React, { Suspense, useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/context/auth-context";
import { Tabs } from "@/components/ui/tabs";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { OverviewTab } from "@/components/portal/overview-tab";
import { OrderMatrixTab } from "@/components/portal/order-matrix-tab";
import { CatalogTab } from "@/components/portal/catalog-tab";
import { OrdersTab } from "@/components/portal/orders-tab";
import { NotificationsTab } from "@/components/portal/notifications-tab";
import { SampleRequestTab } from "@/components/portal/sample-request-tab";
import {
  LayoutDashboard,
  Layers,
  ShoppingBag,
  Clock,
  Bell,
  Gift,
  ShieldCheck,
  UserCheck,
  AlertTriangle,
  ArrowRight,
  ExternalLink,
  Building,
  Lock,
  Phone,
  Mail,
  FileText,
  Truck,
  Percent,
} from "lucide-react";

function PortalContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, profile, isAuthenticated, isPartner, loading } = useAuth();

  const tabParam = searchParams.get("tab") || "overview";
  const [activeTab, setActiveTab] = useState(tabParam);

  // Sync state with search params if url changes
  useEffect(() => {
    if (tabParam) {
      setActiveTab(tabParam);
    }
  }, [tabParam]);

  // Auth Guard
  useEffect(() => {
    if (!loading && !isAuthenticated) {
      router.push("/login?redirect=/portal");
    }
  }, [loading, isAuthenticated, router]);

  const handleTabChange = (tabId: string) => {
    setActiveTab(tabId);
    router.replace(`/portal?tab=${tabId}`, { scroll: false });
  };

  const handleOrderPlaced = (orderCode: string) => {
    setActiveTab("orders");
    router.replace("/portal?tab=orders", { scroll: false });
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
        <Skeleton className="h-14 w-full rounded-2xl" />
        <Skeleton className="h-64 w-full rounded-2xl" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  // Account status verification
  const isAccountActive = user?.accountStatus === "ACTIVE";

  const portalTabs = [
    { id: "overview", label: "Tổng Quan", icon: <LayoutDashboard className="w-4 h-4" /> },
    { id: "matrix", label: "Đặt Hàng Sỉ", icon: <Layers className="w-4 h-4" /> },
    { id: "catalog", label: "Danh Mục Sỉ", icon: <ShoppingBag className="w-4 h-4" /> },
    { id: "orders", label: "Đơn Hàng", icon: <Clock className="w-4 h-4" /> },
    { id: "notifications", label: "Thông Báo", icon: <Bell className="w-4 h-4" /> },
    { id: "samples", label: "Yêu Cầu Mẫu", icon: <Gift className="w-4 h-4" /> },
    { id: "policy", label: "Chính Sách B2B", icon: <ShieldCheck className="w-4 h-4" /> },
    { id: "profile", label: "Hồ Sơ Đối Tác", icon: <UserCheck className="w-4 h-4" /> },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Account Inactive / Pending Warning Banner */}
      {!isAccountActive && (
        <Card className="p-4 border-amber-500/30 bg-amber-950/20 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400 shrink-0">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-white">
                Tài khoản đang ở trạng thái:{" "}
                <span className="text-amber-400 uppercase font-mono">
                  {user?.accountStatus || "PENDING"}
                </span>
              </h4>
              <p className="text-xs text-slate-400">
                Hồ sơ pháp lý của doanh nghiệp đang được chuyên viên thẩm định. Bạn có thể xem trước bảng giá nhưng chưa thể kích hoạt xuất kho.
              </p>
            </div>
          </div>
          <Link href="/onboarding">
            <Button variant="outline" size="sm" className="text-xs text-amber-300 border-amber-500/40">
              Xem tiến độ thẩm định
              <ArrowRight className="w-3.5 h-3.5 ml-1.5" />
            </Button>
          </Link>
        </Card>
      )}

      {/* Portal Top Bar */}
      <div className="p-5 rounded-3xl border border-slate-800 bg-gradient-to-r from-slate-900 via-slate-900/90 to-slate-950 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-black text-white">
              {profile?.companyName || user?.fullName || "Đối Tác Doanh Nghiệp"}
            </h1>
            <Badge variant="brand" size="sm" className="font-mono">
              B2B PARTNER
            </Badge>
            {isAccountActive && (
              <Badge variant="success" size="sm">
                Đã Phê Duyệt
              </Badge>
            )}
          </div>
          <p className="text-xs text-slate-400">
            Mã số thuế: <strong className="text-slate-300 font-mono">{profile?.taxCode || "Đang cập nhật"}</strong> | Người đại diện:{" "}
            <span className="text-slate-300">{profile?.representativeName || user?.fullName}</span>
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Link href="/policy">
            <Button variant="ghost" size="sm" className="text-xs text-slate-400 hover:text-white">
              <ShieldCheck className="w-4 h-4 mr-1 text-slate-400" />
              Chính sách
            </Button>
          </Link>
          <Link href="/stores">
            <Button variant="ghost" size="sm" className="text-xs text-slate-400 hover:text-white">
              <Building className="w-4 h-4 mr-1 text-slate-400" />
              Hệ thống kho
            </Button>
          </Link>
          <Link href="/profile">
            <Button variant="outline" size="sm" className="text-xs">
              <UserCheck className="w-4 h-4 mr-1 text-brand-400" />
              Hồ sơ pháp lý
            </Button>
          </Link>
        </div>
      </div>

      {/* 8-Tab Navigation Bar */}
      <Tabs
        tabs={portalTabs}
        activeTab={activeTab}
        onChange={handleTabChange}
        className="sticky top-16 z-20 bg-slate-950/80 backdrop-blur-md pt-2"
      />

      {/* Tab Panels */}
      <div className="pt-2">
        {activeTab === "overview" && <OverviewTab onNavigateTab={handleTabChange} />}
        {activeTab === "matrix" && <OrderMatrixTab onOrderPlaced={handleOrderPlaced} />}
        {activeTab === "catalog" && <CatalogTab />}
        {activeTab === "orders" && <OrdersTab />}
        {activeTab === "notifications" && <NotificationsTab />}
        {activeTab === "samples" && <SampleRequestTab onOrderPlaced={handleOrderPlaced} />}

        {/* Tab 6: B2B Policy */}
        {activeTab === "policy" && (
          <div className="space-y-6">
            <div className="p-6 rounded-2xl border border-slate-800 bg-slate-900/60 space-y-4">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <ShieldCheck className="w-5 h-5 text-brand-400" />
                    Chính Sách Đối Tác Doanh Nghiệp (B2B)
                  </h2>
                  <p className="text-xs text-slate-400 mt-1">
                    Quy chuẩn chiết khấu sỉ, hạn mức hàng mẫu và nguyên tắc giao nhận kho
                  </p>
                </div>
                <Link href="/policy" target="_blank">
                  <Button variant="outline" size="sm" className="text-xs">
                    Xem toàn văn
                    <ExternalLink className="w-3.5 h-3.5 ml-1.5" />
                  </Button>
                </Link>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Card className="p-4 border-slate-800 bg-slate-950 space-y-2">
                  <div className="flex items-center gap-2 text-xs font-bold text-brand-400">
                    <Percent className="w-4 h-4" />
                    Chiết Khấu Đơn Hàng Sỉ
                  </div>
                  <p className="text-xs text-slate-300 leading-relaxed">
                    Hệ thống tự động áp dụng bậc chiết khấu sỉ (Tier 1 – Tier 3) khi đơn hàng đạt đủ số lượng thùng tối thiểu theo quy định từng sản phẩm.
                  </p>
                </Card>

                <Card className="p-4 border-slate-800 bg-slate-950 space-y-2">
                  <div className="flex items-center gap-2 text-xs font-bold text-amber-400">
                    <Gift className="w-4 h-4" />
                    Chính Sách Hàng Mẫu Thử
                  </div>
                  <p className="text-xs text-slate-300 leading-relaxed">
                    Tiền hàng mẫu = 0 VNĐ (tối đa 1–3 mẫu/sản phẩm). Doanh nghiệp chỉ thanh toán cước phí chuyển phát nhanh thực tế tới địa chỉ nhận.
                  </p>
                </Card>

                <Card className="p-4 border-slate-800 bg-slate-950 space-y-2">
                  <div className="flex items-center gap-2 text-xs font-bold text-emerald-400">
                    <Truck className="w-4 h-4" />
                    Giao Vận & Xuất Kho
                  </div>
                  <p className="text-xs text-slate-300 leading-relaxed">
                    Hỗ trợ lấy hàng trực tiếp tại kho (Self-pickup) hoặc giao hàng liên kết qua GHN, GHTK, xe tải AhaMove có đối soát vận đơn thời gian thực.
                  </p>
                </Card>
              </div>
            </div>
          </div>
        )}

        {/* Tab 8: Partner Profile Summary */}
        {activeTab === "profile" && (
          <div className="space-y-6">
            <Card className="p-6 border-slate-800 bg-slate-900/60 space-y-6">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                <div className="flex items-center gap-3">
                  <div className="p-3 rounded-2xl bg-brand-500/10 text-brand-400 border border-brand-500/20">
                    <Building className="w-6 h-6" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-white">
                      {profile?.companyName || "Chưa cập nhật tên công ty"}
                    </h2>
                    <p className="text-xs text-slate-400">
                      Tài khoản liên kết: {user?.email} | Số điện thoại: {user?.phone}
                    </p>
                  </div>
                </div>

                <Link href="/profile">
                  <Button variant="outline" size="sm" className="text-xs">
                    Chỉnh sửa & Đổi mật khẩu
                    <ArrowRight className="w-3.5 h-3.5 ml-1.5" />
                  </Button>
                </Link>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80 space-y-3">
                  <h4 className="text-slate-400 font-semibold uppercase tracking-wider text-[11px]">
                    Thông tin pháp lý doanh nghiệp
                  </h4>
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-slate-400">Mã số thuế:</span>
                      <span className="font-mono text-white font-bold">{profile?.taxCode || "—"}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Người đại diện:</span>
                      <span className="text-white">{profile?.representativeName || "—"}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Giấy phép kinh doanh:</span>
                      <span className="font-mono text-slate-300">
                        {profile?.businessLicenseUrl ? "Đã đính kèm hồ sơ" : "—"}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Địa chỉ đăng ký:</span>
                      <span className="text-white text-right max-w-xs truncate">
                        {profile?.addressLine || "—"}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80 space-y-3">
                  <h4 className="text-slate-400 font-semibold uppercase tracking-wider text-[11px]">
                    Thông tin giao nhận mặc định
                  </h4>
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-slate-400">Người nhận hàng:</span>
                      <span className="text-white font-bold">
                        {profile?.contactPersonName || profile?.representativeName || "—"}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Số điện thoại nhận:</span>
                      <span className="font-mono text-white">{profile?.contactPhone || "—"}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Kho nhận hàng:</span>
                      <span className="text-white text-right max-w-xs truncate">
                        {profile?.shippingAddressLine || profile?.addressLine || "—"}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Ghi chú kho nhận:</span>
                      <span className="text-slate-300">
                        {profile?.shippingAddressDetail || "Không có"}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </Card>
          </div>
        )}
      </div>
    </div>
  );
}

export default function PortalPage() {
  return (
    <Suspense
      fallback={
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
          <Skeleton className="h-14 w-full rounded-2xl" />
          <Skeleton className="h-64 w-full rounded-2xl" />
        </div>
      }
    >
      <PortalContent />
    </Suspense>
  );
}
