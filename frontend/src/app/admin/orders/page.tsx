"use client";

import React, { Suspense, useEffect, useState, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { api } from "@/lib/api-client";
import { useToast } from "@/context/toast-context";
import { Order, OrderStatus, OrderType, PaymentStatus, ShippingProvider } from "@/types/order";
import { CursorPageDto } from "@/types/api";
import { AdminUpdateOrderStatusRequest, AdminBulkWaybillItem } from "@/types/admin";
import { formatVnd, formatDate } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Skeleton } from "@/components/ui/skeleton";
import {
  PackageCheck,
  Search,
  Filter,
  Truck,
  CheckCircle2,
  XCircle,
  Eye,
  FileSpreadsheet,
  Building,
  RefreshCw,
  ExternalLink,
  CreditCard,
  Layers,
  Gift,
  Clock,
  Plus,
  Trash2,
} from "lucide-react";

function AdminOrdersContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { success, error } = useToast();

  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  // Filters
  const [filterType, setFilterType] = useState<string>("");
  const [filterStatus, setFilterStatus] = useState<string>("");
  const [filterPayment, setFilterPayment] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState("");

  // Modals
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [dispatchModalOpen, setDispatchModalOpen] = useState(false);
  const [dispatchTrackingCode, setDispatchTrackingCode] = useState("");
  const [bulkWaybillModalOpen, setBulkWaybillModalOpen] = useState(false);

  // Bulk waybill form
  const [bulkRows, setBulkRows] = useState<AdminBulkWaybillItem[]>([
    { orderCode: "", shippingProvider: "GHN", trackingNumber: "" },
  ]);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filterType) params.append("orderType", filterType);
      if (filterStatus) params.append("orderStatus", filterStatus);
      if (filterPayment) params.append("paymentStatus", filterPayment);
      params.append("limit", "50");

      const res = await api.get<CursorPageDto<Order>>(`/api/v1/admin/orders?${params.toString()}`);
      setOrders(res.items || []);
    } catch (err) {
      console.warn("Could not load admin orders", err);
    } finally {
      setLoading(false);
    }
  }, [filterType, filterStatus, filterPayment]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // Update order status single
  const handleUpdateStatus = async (
    orderCode: string,
    targetStatus: OrderStatus,
    trackingNumber?: string
  ) => {
    setActionLoading(true);
    try {
      const payload: AdminUpdateOrderStatusRequest = {
        targetStatus,
        trackingNumber: trackingNumber || undefined,
      };
      await api.patch(`/api/v1/admin/orders/${orderCode}/status`, payload);
      success(`Đơn hàng #${orderCode} đã chuyển sang trạng thái ${targetStatus}`);
      setDispatchModalOpen(false);
      setSelectedOrder(null);
      setDispatchTrackingCode("");
      fetchOrders();
    } catch (err: any) {
      console.error("Update order status failed", err);
      error(err.message || "Không thể cập nhật trạng thái đơn hàng");
    } finally {
      setActionLoading(false);
    }
  };

  // Bulk Waybill Submit
  const handleBulkWaybillSubmit = async () => {
    const validRows = bulkRows.filter((r) => r.orderCode.trim() && r.trackingNumber.trim());
    if (validRows.length === 0) {
      error("Vui lòng nhập ít nhất một dòng mã đơn và mã vận đơn hợp lệ");
      return;
    }

    setActionLoading(true);
    try {
      await api.post("/api/v1/admin/orders/bulk-waybill", validRows);
      success(`Đã xuất kho và gán mã vận đơn thành công cho ${validRows.length} đơn hàng!`);
      setBulkWaybillModalOpen(false);
      setBulkRows([{ orderCode: "", shippingProvider: "GHN", trackingNumber: "" }]);
      fetchOrders();
    } catch (err: any) {
      console.error("Bulk waybill assign failed", err);
      error(err.message || "Không thể cập nhật mã vận đơn hàng loạt");
    } finally {
      setActionLoading(false);
    }
  };

  const getOrderStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case "PENDING":
        return <Badge variant="warning" size="sm">Chờ Duyệt Kho</Badge>;
      case "CONFIRMED":
        return <Badge variant="info" size="sm">Đã Duyệt</Badge>;
      case "PROCESSING":
        return <Badge variant="brand" size="sm">Đang Đóng Gói</Badge>;
      case "SHIPPED":
        return <Badge variant="brand" size="sm">Đang Giao Vận</Badge>;
      case "DELIVERED":
      case "COMPLETED":
        return <Badge variant="success" size="sm">Hoàn Thành</Badge>;
      case "CANCELLED":
        return <Badge variant="danger" size="sm">Đã Hủy</Badge>;
      default:
        return <Badge variant="neutral" size="sm">{status}</Badge>;
    }
  };

  const filteredOrders = orders.filter((o) => {
    if (!searchTerm.trim()) return true;
    const term = searchTerm.toLowerCase();
    return (
      o.orderCode.toLowerCase().includes(term) ||
      (o.trackingNumber && o.trackingNumber.toLowerCase().includes(term)) ||
      o.recipientName?.toLowerCase().includes(term)
    );
  });

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-2.5">
            <PackageCheck className="w-6 h-6 text-brand-400" />
            Xử Lý Đơn Hàng & Xuất Vận Đơn (Fulfillment)
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Duyệt đơn sỉ & mẫu thử, liên kết đối soát vận đơn GHN / AhaMove / GHTK và giải phóng tồn kho
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setBulkWaybillModalOpen(true)}
            className="text-xs border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/10"
          >
            <FileSpreadsheet className="w-4 h-4 mr-1.5" />
            Nhập Mã Vận Đơn Hàng Loạt
          </Button>

          <Button variant="outline" size="sm" onClick={fetchOrders} className="text-xs">
            <RefreshCw className="w-3.5 h-3.5 mr-1" />
            Làm mới
          </Button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 space-y-3">
        <div className="flex flex-col md:flex-row items-center justify-between gap-3">
          {/* Quick status tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto no-scrollbar">
            {[
              { id: "", label: "Tất cả" },
              { id: "PENDING", label: "Chờ duyệt" },
              { id: "CONFIRMED", label: "Đã duyệt" },
              { id: "SHIPPED", label: "Đang giao" },
              { id: "DELIVERED", label: "Hoàn thành" },
              { id: "CANCELLED", label: "Đã hủy" },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setFilterStatus(tab.id)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors ${
                  filterStatus === tab.id
                    ? "bg-brand-600 text-white shadow-sm"
                    : "bg-slate-950 text-slate-400 hover:text-white border border-slate-800"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Search */}
          <div className="w-full md:w-64">
            <Input
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm mã đơn, vận đơn, tên..."
              leftIcon={<Search className="w-4 h-4 text-slate-400" />}
              className="h-9 text-xs bg-slate-950 rounded-xl"
            />
          </div>
        </div>

        {/* Secondary dropdown filters */}
        <div className="flex items-center gap-3 pt-2 border-t border-slate-800/80 text-xs">
          <span className="text-slate-400 font-medium">Lọc thêm:</span>
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1 text-slate-300 focus:outline-none"
          >
            <option value="">Tất cả phân loại (Sỉ & Mẫu)</option>
            <option value="WHOLESALE">Đơn hàng sỉ (WHOLESALE)</option>
            <option value="SAMPLE">Đơn gửi mẫu thử (SAMPLE)</option>
          </select>

          <select
            value={filterPayment}
            onChange={(e) => setFilterPayment(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1 text-slate-300 focus:outline-none"
          >
            <option value="">Tất cả thanh toán</option>
            <option value="PAID">Đã thanh toán (PAID)</option>
            <option value="UNPAID">Chưa thanh toán (UNPAID)</option>
          </select>
        </div>
      </div>

      {/* Orders Table */}
      <Card className="border-slate-800 bg-slate-900/60 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-[11px] font-bold uppercase tracking-wider text-slate-400 select-none">
              <tr>
                <th className="p-4">Mã Đơn & Loại</th>
                <th className="p-4">Kho Xuất & Khách Nhận</th>
                <th className="p-4">Tổng Tiền</th>
                <th className="p-4">Thanh Toán</th>
                <th className="p-4">Vận Chuyển & Vận Đơn</th>
                <th className="p-4">Trạng Thái</th>
                <th className="p-4">Ngày Tạo</th>
                <th className="p-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {loading ? (
                [1, 2, 3, 4, 5].map((i) => (
                  <tr key={i}>
                    <td colSpan={8} className="p-4">
                      <Skeleton className="h-8 w-full rounded-xl" />
                    </td>
                  </tr>
                ))
              ) : filteredOrders.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-12 text-center text-slate-500">
                    <PackageCheck className="w-8 h-8 mx-auto mb-2 opacity-50" />
                    Không tìm thấy đơn hàng nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                filteredOrders.map((order) => (
                  <tr key={order.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="p-4">
                      <div className="font-mono font-bold text-white text-sm">
                        #{order.orderCode}
                      </div>
                      <div className="mt-1">
                        {order.orderType === "SAMPLE" ? (
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/30">
                            MẪU THỬ
                          </span>
                        ) : (
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-brand-500/10 text-brand-300 border border-brand-500/30">
                            ĐƠN SỈ
                          </span>
                        )}
                      </div>
                    </td>

                    <td className="p-4">
                      <div className="font-semibold text-white">{order.recipientName}</div>
                      <div className="text-[11px] text-slate-400 mt-0.5 flex items-center gap-1">
                        <Building className="w-3 h-3 text-slate-500" />
                        Kho: {order.branchName || "Kho Tổng"}
                      </div>
                    </td>

                    <td className="p-4">
                      <div className="font-bold text-white text-sm">
                        {formatVnd(order.totalAmount)}
                      </div>
                      <div className="text-[10px] text-slate-500">
                        {order.items?.length || 0} mục hàng
                      </div>
                    </td>

                    <td className="p-4">
                      <div className="font-mono text-slate-300">{order.paymentMethod}</div>
                      <div className="mt-0.5">
                        {order.paymentStatus === "PAID" ? (
                          <span className="text-[10px] text-emerald-400 font-bold">ĐÃ THANH TOÁN</span>
                        ) : (
                          <span className="text-[10px] text-amber-400">CHƯA THANH TOÁN</span>
                        )}
                      </div>
                    </td>

                    <td className="p-4">
                      <div className="font-medium text-white">{order.shippingProvider}</div>
                      {order.trackingNumber ? (
                        <div className="text-[11px] font-mono text-brand-300 mt-0.5">
                          MÃ: {order.trackingNumber}
                        </div>
                      ) : (
                        <span className="text-[11px] text-slate-500 italic">Chưa có vận đơn</span>
                      )}
                    </td>

                    <td className="p-4">{getOrderStatusBadge(order.orderStatus)}</td>

                    <td className="p-4 text-slate-400 whitespace-nowrap">
                      {formatDate(order.createdAt)}
                    </td>

                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {/* Confirm Button */}
                        {order.orderStatus === "PENDING" && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleUpdateStatus(order.orderCode, "CONFIRMED")}
                            disabled={actionLoading}
                            className="text-xs h-7 px-2 font-bold"
                          >
                            Duyệt kho
                          </Button>
                        )}

                        {/* Dispatch Button */}
                        {(order.orderStatus === "CONFIRMED" || order.orderStatus === "PROCESSING") && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setSelectedOrder(order);
                              setDispatchTrackingCode(order.trackingNumber || "");
                              setDispatchModalOpen(true);
                            }}
                            className="text-xs h-7 px-2 border-brand-500/40 text-brand-300"
                          >
                            <Truck className="w-3.5 h-3.5 mr-1" />
                            Giao vận
                          </Button>
                        )}

                        {/* Complete Delivery */}
                        {order.orderStatus === "SHIPPED" && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleUpdateStatus(order.orderCode, "DELIVERED")}
                            disabled={actionLoading}
                            className="text-xs h-7 px-2 border-emerald-500/40 text-emerald-300"
                          >
                            Giao xong
                          </Button>
                        )}

                        {/* Cancel Button */}
                        {(order.orderStatus === "PENDING" || order.orderStatus === "CONFIRMED") && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleUpdateStatus(order.orderCode, "CANCELLED")}
                            disabled={actionLoading}
                            className="text-xs h-7 px-1.5 text-rose-400 border-rose-500/30"
                          >
                            Hủy
                          </Button>
                        )}

                        {/* View Snapshot */}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setSelectedOrder(order);
                            setDetailModalOpen(true);
                          }}
                          className="text-xs h-7 px-1.5 text-slate-400 hover:text-white"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Dispatch Modal */}
      {dispatchModalOpen && selectedOrder && (
        <Modal
          isOpen={true}
          onClose={() => setDispatchModalOpen(false)}
          title={`Xuất Kho & Giao Vận Đơn #${selectedOrder.orderCode}`}
          maxWidth="md"
        >
          <div className="space-y-4 py-2">
            <p className="text-xs text-slate-300 leading-relaxed">
              Nhập mã vận đơn từ đối tác chuyển phát (GHN / AhaMove / GHTK) để chuyển trạng thái sang <strong>SHIPPED</strong>. Đối tác sẽ nhận được thông báo hành trình thời gian thực.
            </p>

            <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs space-y-1">
              <div>Khách nhận: <strong className="text-white">{selectedOrder.recipientName}</strong> ({selectedOrder.recipientPhone})</div>
              <div>Địa chỉ: <span className="text-slate-300">{selectedOrder.shippingAddressLine}</span></div>
              <div>Đơn vị: <strong className="text-brand-300">{selectedOrder.shippingProvider}</strong></div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">
                Mã Vận Đơn Đối Tác (Tracking Code) *:
              </label>
              <Input
                value={dispatchTrackingCode}
                onChange={(e) => setDispatchTrackingCode(e.target.value)}
                placeholder="VD: GHN-829183921 hoặc AHA-HN-002"
                className="bg-slate-950"
              />
            </div>

            <div className="flex justify-end gap-2.5 pt-2">
              <Button variant="outline" size="sm" onClick={() => setDispatchModalOpen(false)}>
                Hủy bỏ
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={() =>
                  handleUpdateStatus(selectedOrder.orderCode, "SHIPPED", dispatchTrackingCode)
                }
                disabled={actionLoading || !dispatchTrackingCode.trim()}
                className="text-xs font-bold"
              >
                {actionLoading ? "Đang xử lý..." : "Xác Nhận Xuất Kho"}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Bulk Waybill Import Modal */}
      {bulkWaybillModalOpen && (
        <Modal
          isOpen={true}
          onClose={() => setBulkWaybillModalOpen(false)}
          title="Nhập Mã Vận Đơn Hàng Loạt (Bulk Waybills)"
          maxWidth="lg"
        >
          <div className="space-y-4 py-2 text-xs">
            <p className="text-slate-300 leading-relaxed">
              Nhập danh sách mã đơn và mã vận đơn thực tế để chuyển hàng loạt đơn sang trạng thái <strong>SHIPPED</strong>.
            </p>

            <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
              {bulkRows.map((row, idx) => (
                <div key={idx} className="flex items-center gap-2">
                  <Input
                    placeholder="Mã đơn (VD: UM26101301)"
                    value={row.orderCode}
                    onChange={(e) => {
                      const updated = [...bulkRows];
                      updated[idx].orderCode = e.target.value;
                      setBulkRows(updated);
                    }}
                    className="h-8 text-xs bg-slate-950 font-mono"
                  />

                  <select
                    value={row.shippingProvider}
                    onChange={(e) => {
                      const updated = [...bulkRows];
                      updated[idx].shippingProvider = e.target.value as ShippingProvider;
                      setBulkRows(updated);
                    }}
                    className="h-8 bg-slate-950 border border-slate-700 rounded-lg px-2 text-white text-xs"
                  >
                    <option value="GHN">GHN</option>
                    <option value="AHAMOVE">AhaMove</option>
                    <option value="GHTK">GHTK</option>
                  </select>

                  <Input
                    placeholder="Mã vận đơn đối tác"
                    value={row.trackingNumber}
                    onChange={(e) => {
                      const updated = [...bulkRows];
                      updated[idx].trackingNumber = e.target.value;
                      setBulkRows(updated);
                    }}
                    className="h-8 text-xs bg-slate-950 font-mono"
                  />

                  {bulkRows.length > 1 && (
                    <button
                      onClick={() => setBulkRows(bulkRows.filter((_, i) => i !== idx))}
                      className="p-1.5 text-slate-500 hover:text-rose-400"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                setBulkRows([
                  ...bulkRows,
                  { orderCode: "", shippingProvider: "GHN", trackingNumber: "" },
                ])
              }
              className="text-xs"
            >
              <Plus className="w-3.5 h-3.5 mr-1" />
              Thêm dòng
            </Button>

            <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-800">
              <Button variant="outline" size="sm" onClick={() => setBulkWaybillModalOpen(false)}>
                Hủy
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleBulkWaybillSubmit}
                disabled={actionLoading}
                className="text-xs font-bold"
              >
                {actionLoading ? "Đang xử lý..." : "Cập Nhật Toàn Bộ"}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Snapshot Detail Modal */}
      {detailModalOpen && selectedOrder && (
        <Modal
          isOpen={true}
          onClose={() => setDetailModalOpen(false)}
          title={`Chi Tiết Đơn Hàng #${selectedOrder.orderCode}`}
          maxWidth="lg"
        >
          <div className="space-y-4 py-2 text-xs">
            <div className="grid grid-cols-2 gap-4">
              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  Thông Tin Người Nhận
                </span>
                <div>Họ tên: <strong className="text-white">{selectedOrder.recipientName}</strong></div>
                <div>SĐT: <span className="font-mono text-white">{selectedOrder.recipientPhone}</span></div>
                <div>Địa chỉ: <span className="text-slate-300">{selectedOrder.shippingAddressLine}</span></div>
                {selectedOrder.shippingAddressDetail && (
                  <div>Chi tiết: <span className="text-slate-400">{selectedOrder.shippingAddressDetail}</span></div>
                )}
              </div>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  Thanh Toán & Vận Chuyển
                </span>
                <div>Cổng: <strong className="text-white">{selectedOrder.paymentMethod}</strong> ({selectedOrder.paymentStatus})</div>
                <div>Giao nhận: <strong className="text-white">{selectedOrder.shippingProvider}</strong></div>
                {selectedOrder.trackingNumber && (
                  <div>Mã vận đơn: <span className="font-mono text-brand-300">{selectedOrder.trackingNumber}</span></div>
                )}
                <div>Kho xuất: <span className="text-slate-300">{selectedOrder.branchName || "Kho HCMC"}</span></div>
              </div>
            </div>

            {/* Items table */}
            <div className="border border-slate-800 rounded-xl overflow-hidden">
              <table className="w-full text-left">
                <thead className="bg-slate-950 text-slate-400 text-[10px] uppercase font-bold">
                  <tr>
                    <th className="p-2.5">Sản phẩm</th>
                    <th className="p-2.5 text-center">Bậc giá sỉ</th>
                    <th className="p-2.5 text-center">Số lượng</th>
                    <th className="p-2.5 text-right">Đơn giá</th>
                    <th className="p-2.5 text-right">Thành tiền</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {selectedOrder.items?.map((item) => (
                    <tr key={item.id}>
                      <td className="p-2.5">
                        <div className="font-bold text-white">{item.productNameSnap}</div>
                        <div className="text-[10px] font-mono text-slate-400">SKU: {item.skuSnapshot}</div>
                      </td>
                      <td className="p-2.5 text-center">
                        <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px]">
                          {item.appliedTierCode || "TIER 1"}
                        </span>
                      </td>
                      <td className="p-2.5 text-center font-bold text-white">
                        {item.quantity} {item.unitLabelSnap}
                      </td>
                      <td className="p-2.5 text-right text-slate-300">
                        {formatVnd(item.unitPrice)}
                      </td>
                      <td className="p-2.5 text-right font-bold text-white">
                        {formatVnd(item.lineTotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-between items-center pt-2 border-t border-slate-800">
              <div className="text-slate-400">
                Cước vận chuyển: <strong className="text-white">{formatVnd(selectedOrder.shippingFee)}</strong>
              </div>
              <div className="text-sm font-bold text-white">
                Tổng thanh toán: <span className="text-brand-400 text-base">{formatVnd(selectedOrder.totalAmount)}</span>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <Button variant="outline" size="sm" onClick={() => setDetailModalOpen(false)}>
                Đóng
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

export default function AdminOrdersPage() {
  return (
    <Suspense
      fallback={
        <div className="space-y-6">
          <Skeleton className="h-10 w-64 rounded-xl" />
          <Skeleton className="h-96 w-full rounded-2xl" />
        </div>
      }
    >
      <AdminOrdersContent />
    </Suspense>
  );
}
