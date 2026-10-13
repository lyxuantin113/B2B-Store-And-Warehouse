"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api-client";
import { AdminSalesAnalyticsKpi, AdminPartnerKpi, BranchInventory } from "@/types/admin";
import { formatVnd, formatNumber } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DollarSign,
  ShoppingBag,
  TrendingUp,
  CreditCard,
  Truck,
  AlertTriangle,
  Users,
  Boxes,
  ArrowRight,
  ShieldCheck,
  Building,
  CheckCircle2,
  Clock,
  Package,
} from "lucide-react";

export default function AdminDashboardPage() {
  const [salesKpis, setSalesKpis] = useState<AdminSalesAnalyticsKpi | null>(null);
  const [partnerKpis, setPartnerKpis] = useState<AdminPartnerKpi | null>(null);
  const [lowStockAlerts, setLowStockAlerts] = useState<BranchInventory[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const [sales, partners, lowStock] = await Promise.all([
          api.get<AdminSalesAnalyticsKpi>("/api/v1/admin/orders/analytics/kpis"),
          api.get<AdminPartnerKpi>("/api/v1/admin/partners/kpis"),
          api.get<BranchInventory[]>("/api/v1/admin/inventory/low-stock-alerts"),
        ]);
        setSalesKpis(sales);
        setPartnerKpis(partners);
        setLowStockAlerts(lowStock);
      } catch (err) {
        console.warn("Could not load admin dashboard data", err);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-64 rounded-xl" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <Skeleton key={i} className="h-28 rounded-2xl" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Page Title */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            Bảng Điều Khiển Quản Trị Hệ Thống
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Tổng quan hiệu suất bán hàng sỉ, tiến độ thẩm định đối tác B2B và giám sát tồn kho
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Link href="/admin/partners">
            <Button variant="outline" size="sm" className="text-xs">
              <Users className="w-3.5 h-3.5 mr-1.5 text-brand-400" />
              Thẩm định đối tác ({partnerKpis?.pendingReviewCount || 0})
            </Button>
          </Link>
          <Link href="/admin/orders">
            <Button variant="primary" size="sm" className="text-xs font-bold">
              <Package className="w-3.5 h-3.5 mr-1.5" />
              Xử lý đơn hàng
            </Button>
          </Link>
        </div>
      </div>

      {/* 6 Executive Sales KPIs */}
      <div className="space-y-3">
        <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
          <TrendingUp className="w-4 h-4 text-brand-400" />
          Chỉ Số Kinh Doanh Sỉ Thời Gian Thực
        </span>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {/* Card 1: Doanh Thu */}
          <Card className="p-5 border-slate-800 bg-slate-900/70 hover:border-slate-700 transition-all space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span className="font-medium">Tổng Doanh Thu Đơn Sỉ</span>
              <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
                <DollarSign className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-white">
              {formatVnd(salesKpis?.totalRevenue || 0)}
            </div>
            <div className="text-[11px] text-emerald-400 flex items-center gap-1 font-semibold">
              <TrendingUp className="w-3 h-3" />
              Doanh thu lũy kế các đơn thành công
            </div>
          </Card>

          {/* Card 2: Tổng Đơn Hàng */}
          <Card className="p-5 border-slate-800 bg-slate-900/70 hover:border-slate-700 transition-all space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span className="font-medium">Tổng Đơn Hàng Tiếp Nhận</span>
              <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400">
                <ShoppingBag className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-white">
              {formatNumber(salesKpis?.totalOrders || 0)}{" "}
              <span className="text-xs font-normal text-slate-400">đơn</span>
            </div>
            <div className="text-[11px] text-slate-400 flex items-center gap-1">
              Bao gồm đơn sỉ khối lượng lớn & mẫu thử
            </div>
          </Card>

          {/* Card 3: Giá Trị Đơn Trung Bình (AOV) */}
          <Card className="p-5 border-slate-800 bg-slate-900/70 hover:border-slate-700 transition-all space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span className="font-medium">Giá Trị Trung Bình / Đơn (AOV)</span>
              <div className="p-2 rounded-xl bg-purple-500/10 text-purple-400">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-white">
              {formatVnd(salesKpis?.averageOrderValue || 0)}
            </div>
            <div className="text-[11px] text-purple-400 font-semibold">
              Chi tiêu trung bình mỗi đơn sỉ B2B
            </div>
          </Card>

          {/* Card 4: Đơn Đã Thanh Toán */}
          <Card className="p-5 border-slate-800 bg-slate-900/70 hover:border-slate-700 transition-all space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span className="font-medium">Đơn Đã Quyết Toán (PAID)</span>
              <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
                <CreditCard className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-white">
              {formatNumber(salesKpis?.paidOrdersCount || 0)}{" "}
              <span className="text-xs font-normal text-slate-400">giao dịch</span>
            </div>
            <div className="text-[11px] text-slate-400">
              Thanh toán tự động qua VNPay / MoMo / VietQR
            </div>
          </Card>

          {/* Card 5: Tỷ Lệ Giao Hàng Thành Công */}
          <Card className="p-5 border-slate-800 bg-slate-900/70 hover:border-slate-700 transition-all space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span className="font-medium">Tỷ Lệ Giao Thành Công</span>
              <div className="p-2 rounded-xl bg-sky-500/10 text-sky-400">
                <Truck className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-white">
              {salesKpis?.deliveryCompletionRatePercent ?? 100}%
            </div>
            <div className="text-[11px] text-sky-400 font-semibold">
              Vận đơn đối soát qua GHN & AhaMove
            </div>
          </Card>

          {/* Card 6: Tỷ Lệ Hủy Đơn */}
          <Card className="p-5 border-slate-800 bg-slate-900/70 hover:border-slate-700 transition-all space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span className="font-medium">Tỷ Lệ Hủy Đơn</span>
              <div className="p-2 rounded-xl bg-rose-500/10 text-rose-400">
                <AlertTriangle className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-black text-white">
              {salesKpis?.cancellationRatePercent ?? 0}%
            </div>
            <div className="text-[11px] text-slate-400">
              Bao gồm quá hạn thanh toán 24h & hủy kho
            </div>
          </Card>
        </div>
      </div>

      {/* Operational Highlights Grid: Partner CRM Status + Low Stock Alerts */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left: Partner CRM Overview (6 cols) */}
        <div className="lg:col-span-6 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <Users className="w-4 h-4 text-brand-400" />
              Mạng Lưới Đối Tác Doanh Nghiệp
            </span>
            <Link href="/admin/partners" className="text-xs text-brand-400 hover:underline">
              Quản lý chi tiết →
            </Link>
          </div>

          <Card className="p-5 border-slate-800 bg-slate-900/60 space-y-4">
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="p-3 rounded-2xl bg-slate-950 border border-slate-800">
                <div className="text-xs text-slate-400">Tổng Đối Tác</div>
                <div className="text-xl font-black text-white mt-1">
                  {partnerKpis?.totalPartners || 0}
                </div>
              </div>
              <div className="p-3 rounded-2xl bg-amber-500/10 border border-amber-500/20">
                <div className="text-xs text-amber-400">Chờ Thẩm Định</div>
                <div className="text-xl font-black text-amber-300 mt-1">
                  {partnerKpis?.pendingReviewCount || 0}
                </div>
              </div>
              <div className="p-3 rounded-2xl bg-emerald-500/10 border border-emerald-500/20">
                <div className="text-xs text-emerald-400">Đã Kích Hoạt</div>
                <div className="text-xl font-black text-emerald-300 mt-1">
                  {partnerKpis?.activeCount || 0}
                </div>
              </div>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between text-xs">
              <div className="flex items-center gap-2.5">
                <Clock className="w-4 h-4 text-amber-400" />
                <span className="text-slate-300">
                  Có <strong>{partnerKpis?.pendingReviewCount || 0}</strong> doanh nghiệp đang nộp hồ sơ GPKD chờ duyệt
                </span>
              </div>
              <Link href="/admin/partners?status=PENDING_ADMIN">
                <Button variant="outline" size="sm" className="text-xs py-1 h-7">
                  Duyệt ngay
                </Button>
              </Link>
            </div>
          </Card>
        </div>

        {/* Right: Low Stock Alert Watchlist (6 cols) */}
        <div className="lg:col-span-6 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-rose-400 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4" />
              Cảnh Báo Tồn Kho Sắp Hết ({lowStockAlerts.length})
            </span>
            <Link href="/admin/inventory" className="text-xs text-brand-400 hover:underline">
              Điều chuyển kho →
            </Link>
          </div>

          <Card className="p-5 border-slate-800 bg-slate-900/60 space-y-3">
            {lowStockAlerts.length === 0 ? (
              <div className="py-8 text-center text-slate-400 text-xs">
                <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto mb-2 opacity-80" />
                Mọi chi nhánh đều duy trì mức tồn kho an toàn.
              </div>
            ) : (
              <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                {lowStockAlerts.slice(0, 5).map((item) => (
                  <div
                    key={`${item.branchId}-${item.productId}`}
                    className="p-3 rounded-xl bg-rose-950/20 border border-rose-500/20 flex items-center justify-between text-xs"
                  >
                    <div>
                      <div className="font-bold text-white">{item.productName}</div>
                      <div className="text-[11px] text-slate-400">
                        Kho: <strong className="text-slate-300">{item.branchName}</strong> | SKU:{" "}
                        <span className="font-mono text-brand-300">{item.productSku}</span>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-bold text-rose-400">
                        {item.availableQty} {item.unitLabel}
                      </div>
                      <div className="text-[10px] text-slate-400">
                        Mức an toàn: {item.safetyStock}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
