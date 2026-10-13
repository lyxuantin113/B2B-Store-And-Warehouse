"use client";

import React, { useState, useEffect } from "react";
import { Order, OrderStatus, OrderType } from "@/types/order";
import { CursorPageDto } from "@/types/api";
import { api } from "@/lib/api-client";
import { useToast } from "@/context/toast-context";
import { formatVnd, formatDate } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Modal } from "@/components/ui/modal";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Package,
  Truck,
  ExternalLink,
  Ban,
  Clock,
  CheckCircle2,
  AlertCircle,
  FileText,
  Search,
} from "lucide-react";

export function OrdersTab() {
  const { success, error } = useToast();

  const [orders, setOrders] = useState<Order[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [orderTypeFilter, setOrderTypeFilter] = useState<string>("");
  const [orderStatusFilter, setOrderStatusFilter] = useState<string>("");
  const [cancellingCode, setCancellingCode] = useState<string | null>(null);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (orderTypeFilter) params.set("orderType", orderTypeFilter);
      if (orderStatusFilter) params.set("orderStatus", orderStatusFilter);
      params.set("limit", "50");

      const res = await api.get<CursorPageDto<Order>>(`/api/v1/b2b/orders?${params.toString()}`);
      setOrders(res.items || []);
    } catch (err: any) {
      console.warn("Could not load orders", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [orderTypeFilter, orderStatusFilter]);

  const handleCancelOrder = async (orderCode: string) => {
    if (!confirm(`Bạn có chắc muốn hủy đơn hàng #${orderCode}? Tồn kho sẽ được hoàn trả lập tức.`)) {
      return;
    }

    setCancellingCode(orderCode);
    try {
      const cancelled = await api.post<Order>(`/api/v1/b2b/orders/${orderCode}/cancel`);
      success(`Đã hủy đơn hàng #${orderCode} thành công.`);
      setOrders((prev) =>
        prev.map((o) => (o.orderCode === orderCode ? { ...o, orderStatus: "CANCELLED" } : o))
      );
      if (selectedOrder?.orderCode === orderCode) {
        setSelectedOrder((o) => (o ? { ...o, orderStatus: "CANCELLED" } : null));
      }
    } catch (err: any) {
      error(err.message || "Hủy đơn hàng thất bại.");
    } finally {
      setCancellingCode(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Filters Toolbar */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">
            Loại đơn:
          </span>
          <select
            value={orderTypeFilter}
            onChange={(e) => setOrderTypeFilter(e.target.value)}
            className="bg-slate-950 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none"
          >
            <option value="">Tất cả loại đơn</option>
            <option value="WHOLESALE">Đơn sỉ (Wholesale)</option>
            <option value="SAMPLE">Đơn mẫu thử (Sample)</option>
          </select>

          <span className="text-xs font-bold text-slate-300 uppercase tracking-wider ml-2">
            Trạng thái:
          </span>
          <select
            value={orderStatusFilter}
            onChange={(e) => setOrderStatusFilter(e.target.value)}
            className="bg-slate-950 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="PENDING">Chờ xử lý (PENDING)</option>
            <option value="CONFIRMED">Đã xác nhận (CONFIRMED)</option>
            <option value="PROCESSING">Đang chuẩn bị (PROCESSING)</option>
            <option value="SHIPPED">Đang giao (SHIPPED)</option>
            <option value="DELIVERED">Đã giao (DELIVERED)</option>
            <option value="COMPLETED">Hoàn tất (COMPLETED)</option>
            <option value="CANCELLED">Đã hủy (CANCELLED)</option>
          </select>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={fetchOrders}
          className="text-xs text-slate-300 hover:text-white"
        >
          Làm mới danh sách
        </Button>
      </div>

      {/* Orders Table */}
      <Card className="p-6 border-slate-800 bg-slate-900/60 space-y-4">
        {loading ? (
          <div className="space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <Skeleton key={i} className="h-14 w-full rounded-xl" />
            ))}
          </div>
        ) : orders.length === 0 ? (
          <div className="p-12 text-center text-xs text-slate-500 space-y-2">
            <Package className="w-10 h-10 text-slate-700 mx-auto" />
            <p>Không tìm thấy đơn hàng nào phù hợp với bộ lọc.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider text-[11px]">
                  <th className="py-3 px-3">Mã đơn hàng</th>
                  <th className="py-3 px-3">Phân loại</th>
                  <th className="py-3 px-3">Vận chuyển</th>
                  <th className="py-3 px-3">Mã vận đơn</th>
                  <th className="py-3 px-3">Tổng thanh toán</th>
                  <th className="py-3 px-3">Thanh toán</th>
                  <th className="py-3 px-3">Trạng thái</th>
                  <th className="py-3 px-3">Ngày tạo</th>
                  <th className="py-3 px-3 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-slate-200">
                {orders.map((ord) => (
                  <tr key={ord.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-3.5 px-3 font-mono font-bold text-brand-400">
                      #{ord.orderCode}
                    </td>
                    <td className="py-3.5 px-3">
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          ord.orderType === "SAMPLE"
                            ? "bg-emerald-950 text-emerald-400 border border-emerald-800"
                            : "bg-blue-950 text-blue-400 border border-blue-800"
                        }`}
                      >
                        {ord.orderType === "SAMPLE" ? "MẪU THỬ" : "ĐƠN SỈ"}
                      </span>
                    </td>
                    <td className="py-3.5 px-3 text-slate-300">
                      {ord.shippingProvider}
                    </td>
                    <td className="py-3.5 px-3 font-mono text-slate-300">
                      {ord.trackingNumber ? (
                        <span className="text-sky-400 font-semibold">{ord.trackingNumber}</span>
                      ) : (
                        <span className="text-slate-500">-</span>
                      )}
                    </td>
                    <td className="py-3.5 px-3 font-bold text-white">
                      {formatVnd(ord.totalAmount)}
                    </td>
                    <td className="py-3.5 px-3">
                      <span
                        className={`font-semibold ${
                          ord.paymentStatus === "PAID" ? "text-emerald-400" : "text-amber-400"
                        }`}
                      >
                        {ord.paymentStatus}
                      </span>
                    </td>
                    <td className="py-3.5 px-3">
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
                    <td className="py-3.5 px-3 text-slate-400">{formatDate(ord.createdAt)}</td>
                    <td className="py-3.5 px-3 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setSelectedOrder(ord)}
                          className="text-xs text-brand-300 hover:text-white"
                        >
                          Chi tiết
                        </Button>
                        {ord.orderStatus === "PENDING" && (
                          <Button
                            variant="danger"
                            size="sm"
                            loading={cancellingCode === ord.orderCode}
                            onClick={() => handleCancelOrder(ord.orderCode)}
                            className="text-xs h-7 px-2.5"
                          >
                            Hủy
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Order Details Modal */}
      {selectedOrder && (
        <Modal
          isOpen={!!selectedOrder}
          onClose={() => setSelectedOrder(null)}
          title={`Chi Tiết Đơn Hàng #${selectedOrder.orderCode}`}
          maxWidth="lg"
        >
          <div className="space-y-5 text-xs">
            {/* Meta status header */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-3.5 rounded-xl bg-slate-950 border border-slate-800">
              <div>
                <span className="text-slate-400 block">Loại đơn:</span>
                <span className="font-bold text-white">{selectedOrder.orderType}</span>
              </div>
              <div>
                <span className="text-slate-400 block">Trạng thái:</span>
                <span className="font-bold text-emerald-400">{selectedOrder.orderStatus}</span>
              </div>
              <div>
                <span className="text-slate-400 block">Thanh toán:</span>
                <span className="font-bold text-amber-400">
                  {selectedOrder.paymentStatus} ({selectedOrder.paymentMethod})
                </span>
              </div>
              <div>
                <span className="text-slate-400 block">Đơn vị vận chuyển:</span>
                <span className="font-bold text-sky-400">
                  {selectedOrder.shippingProvider}
                  {selectedOrder.trackingNumber ? ` - ${selectedOrder.trackingNumber}` : ""}
                </span>
              </div>
            </div>

            {/* Recipient & Shipping */}
            <div className="space-y-1">
              <span className="font-bold text-slate-300 uppercase tracking-wider block">
                Địa chỉ nhận hàng:
              </span>
              <p className="text-slate-200">
                <strong>{selectedOrder.recipientName}</strong> ({selectedOrder.recipientPhone}) —{" "}
                {selectedOrder.shippingAddressLine}
                {selectedOrder.shippingAddressDetail && ` (${selectedOrder.shippingAddressDetail})`}
              </p>
            </div>

            {/* Order Items Table */}
            <div className="space-y-2">
              <span className="font-bold text-slate-300 uppercase tracking-wider block">
                Danh sách mặt hàng snapshot:
              </span>
              <div className="rounded-xl border border-slate-800 overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-950 text-slate-400">
                    <tr className="border-b border-slate-800">
                      <th className="p-2.5">Sản phẩm</th>
                      <th className="p-2.5">Đơn giá bậc</th>
                      <th className="p-2.5">Số lượng</th>
                      <th className="p-2.5 text-right">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/80">
                    {selectedOrder.items?.map((item, idx) => (
                      <tr key={idx}>
                        <td className="p-2.5">
                          <span className="font-bold text-white block">{item.productNameSnap}</span>
                          <span className="text-[10px] font-mono text-slate-400">
                            SKU: {item.skuSnapshot}
                          </span>
                        </td>
                        <td className="p-2.5 text-slate-300">{formatVnd(item.unitPrice)}</td>
                        <td className="p-2.5 font-bold text-white">
                          {item.quantity} {item.unitLabelSnap}
                        </td>
                        <td className="p-2.5 text-right font-bold text-emerald-400">
                          {formatVnd(item.lineTotal)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Cost breakdown */}
            <div className="pt-2 border-t border-slate-800 space-y-1.5 text-right">
              <div className="text-slate-400">
                Tiền hàng: <strong className="text-white">{formatVnd(selectedOrder.subtotalAmount)}</strong>
              </div>
              <div className="text-slate-400">
                Phí vận chuyển:{" "}
                <strong className="text-white">
                  {selectedOrder.shippingFee === 0 ? "Miễn phí" : formatVnd(selectedOrder.shippingFee)}
                </strong>
              </div>
              <div className="text-sm font-black text-white pt-1">
                Tổng thanh toán:{" "}
                <span className="text-emerald-400 text-base">{formatVnd(selectedOrder.totalAmount)}</span>
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
