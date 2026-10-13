"use client";

import React, { useEffect, useState } from "react";
import { useAuth } from "@/context/auth-context";
import { api } from "@/lib/api-client";
import { PartnerDashboardOverview } from "@/types/order";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { formatVnd, formatDate } from "@/lib/utils";
import {
  DollarSign,
  ShoppingBag,
  Truck,
  Gift,
  Building2,
  ArrowRight,
  TrendingUp,
  Clock,
  CheckCircle2,
} from "lucide-react";

interface OverviewTabProps {
  onNavigateTab: (tabId: string) => void;
}

export function OverviewTab({ onNavigateTab }: OverviewTabProps) {
  const { user, profile } = useAuth();
  const [data, setData] = useState<PartnerDashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOverview = async () => {
      try {
        const res = await api.get<PartnerDashboardOverview>("/api/v1/b2b/dashboard/overview");
        setData(res);
      } catch (err) {
        console.warn("Could not load dashboard overview", err);
      } finally {
        setLoading(false);
      }
    };
    fetchOverview();
  }, []);

  return (
    <div className="space-y-8">
      {/* Welcome Banner Card */}
      <div className="p-6 sm:p-8 rounded-3xl border border-slate-800 bg-gradient-to-r from-slate-900 via-slate-900/90 to-brand-950/40 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-2.5 flex-wrap">
            <span className="w-8 h-8 rounded-xl bg-brand-600/30 border border-brand-500/40 flex items-center justify-center text-brand-400">
              <Building2 className="w-4 h-4" />
            </span>
            <h1 className="text-xl sm:text-2xl font-black text-white">
              {profile?.companyName || user?.fullName || "Quý Đối tác"}
            </h1>
            <Badge variant="success" size="sm" className="gap-1">
              <CheckCircle2 className="w-3 h-3 text-emerald-400" /> Đối tác B2B Chính thức
            </Badge>
          </div>
          <p className="text-xs text-slate-400 max-w-xl">
            Mã số thuế: <strong className="text-white font-mono">{profile?.taxCode || "N/A"}</strong> • Người liên hệ: <strong className="text-white">{profile?.contactPersonName || user?.fullName}</strong> ({profile?.contactPhone || "Chưa cập nhật"})
          </p>
        </div>

        <div className="flex items-center gap-3 shrink-0">
          <Button
            variant="primary"
            size="md"
            onClick={() => onNavigateTab("matrix")}
            className="gap-2 shadow-lg shadow-brand-600/25"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>Đặt sỉ nhanh</span>
          </Button>
          <Button
            variant="outline"
            size="md"
            onClick={() => onNavigateTab("samples")}
            className="gap-2 border-slate-700"
          >
            <Gift className="w-4 h-4 text-emerald-400" />
            <span>Nhận mẫu thử 0đ</span>
          </Button>
        </div>
      </div>

      {/* 4 KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <Card className="p-5 border-slate-800 bg-slate-900/70 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Tổng chi tiêu tích lũy
            </span>
            <div className="w-9 h-9 rounded-xl bg-brand-500/10 flex items-center justify-center text-brand-400">
              <DollarSign className="w-5 h-5" />
            </div>
          </div>
          <div>
            {loading ? (
              <Skeleton className="h-8 w-2/3" />
            ) : (
              <span className="text-2xl font-black text-white tracking-tight">
                {formatVnd(data?.totalCumulativeSpend || 0)}
              </span>
            )}
            <p className="text-[11px] text-emerald-400 font-medium mt-1 flex items-center gap-1">
              <TrendingUp className="w-3 h-3" /> Hạn mức chiết khấu Tier 3
            </p>
          </div>
        </Card>

        <Card className="p-5 border-slate-800 bg-slate-900/70 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Tổng đơn sỉ đã đặt
            </span>
            <div className="w-9 h-9 rounded-xl bg-sky-500/10 flex items-center justify-center text-sky-400">
              <ShoppingBag className="w-5 h-5" />
            </div>
          </div>
          <div>
            {loading ? (
              <Skeleton className="h-8 w-1/3" />
            ) : (
              <span className="text-2xl font-black text-white tracking-tight">
                {data?.totalOrdersCount || 0}
              </span>
            )}
            <p className="text-[11px] text-slate-400 mt-1">
              Đơn hàng sỉ B2B toàn hệ thống
            </p>
          </div>
        </Card>

        <Card className="p-5 border-slate-800 bg-slate-900/70 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Đơn đang xử lý & giao
            </span>
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-400">
              <Truck className="w-5 h-5" />
            </div>
          </div>
          <div>
            {loading ? (
              <Skeleton className="h-8 w-1/3" />
            ) : (
              <span className="text-2xl font-black text-white tracking-tight">
                {data?.activeInTransitOrdersCount || 0}
              </span>
            )}
            <p className="text-[11px] text-amber-400 mt-1 flex items-center gap-1">
              <Clock className="w-3 h-3" /> Vận chuyển GHN / AhaMove
            </p>
          </div>
        </Card>

        <Card className="p-5 border-slate-800 bg-slate-900/70 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Đơn mẫu thử đã nhận
            </span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 flex items-center justify-center text-emerald-400">
              <Gift className="w-5 h-5" />
            </div>
          </div>
          <div>
            {loading ? (
              <Skeleton className="h-8 w-1/3" />
            ) : (
              <span className="text-2xl font-black text-white tracking-tight">
                {data?.sampleOrdersCount || 0}
              </span>
            )}
            <p className="text-[11px] text-emerald-400 mt-1">
              Mẫu thử 0đ kiểm định chất lượng
            </p>
          </div>
        </Card>
      </div>

      {/* Recent Orders Section */}
      <Card className="p-6 border-slate-800 bg-slate-900/60 backdrop-blur-sm space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-slate-800">
          <div>
            <h3 className="font-bold text-sm text-white">Đơn Hàng Gần Đây</h3>
            <p className="text-xs text-slate-400">Theo dõi tiến độ giao hàng và mã vận đơn thời gian thực</p>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onNavigateTab("orders")}
            className="text-xs text-brand-400 hover:text-brand-300 gap-1"
          >
            <span>Xem toàn bộ lịch sử</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Button>
        </div>

        {loading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : !data?.recentOrders || data.recentOrders.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500 space-y-2">
            <ShoppingBag className="w-8 h-8 text-slate-700 mx-auto" />
            <p>Chưa có đơn hàng nào được tạo. Bắt đầu đơn hàng sỉ đầu tiên!</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider text-[11px]">
                  <th className="py-2.5 px-3">Mã đơn</th>
                  <th className="py-2.5 px-3">Loại đơn</th>
                  <th className="py-2.5 px-3">Kho xuất</th>
                  <th className="py-2.5 px-3">Tổng tiền</th>
                  <th className="py-2.5 px-3">Trạng thái</th>
                  <th className="py-2.5 px-3">Thanh toán</th>
                  <th className="py-2.5 px-3">Ngày tạo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-slate-200">
                {data.recentOrders.map((ord) => (
                  <tr key={ord.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-3 px-3 font-mono font-bold text-brand-400">
                      {ord.orderCode}
                    </td>
                    <td className="py-3 px-3">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        ord.orderType === "SAMPLE"
                          ? "bg-emerald-950 text-emerald-400 border border-emerald-800"
                          : "bg-blue-950 text-blue-400 border border-blue-800"
                      }`}>
                        {ord.orderType === "SAMPLE" ? "MẪU THỬ" : "ĐƠN SỈ"}
                      </span>
                    </td>
                    <td className="py-3 px-3 text-slate-300">{ord.branchName || "Kho HCM"}</td>
                    <td className="py-3 px-3 font-semibold text-white">
                      {formatVnd(ord.totalAmount)}
                    </td>
                    <td className="py-3 px-3">
                      <Badge
                        variant={
                          ord.orderStatus === "COMPLETED" || ord.orderStatus === "DELIVERED"
                            ? "success"
                            : ord.orderStatus === "CANCELLED"
                            ? "danger"
                            : "warning"
                        }
                        size="sm"
                      >
                        {ord.orderStatus}
                      </Badge>
                    </td>
                    <td className="py-3 px-3">
                      <span className={`font-medium ${ord.paymentStatus === "PAID" ? "text-emerald-400" : "text-amber-400"}`}>
                        {ord.paymentStatus} ({ord.paymentMethod})
                      </span>
                    </td>
                    <td className="py-3 px-3 text-slate-400">{formatDate(ord.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
