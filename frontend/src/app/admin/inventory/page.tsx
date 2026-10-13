"use client";

import React, { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api-client";
import { useToast } from "@/context/toast-context";
import { BranchInventory, UpsertBranchInventoryRequest } from "@/types/admin";
import { Branch } from "@/types/branch";
import { formatDate } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Boxes,
  Building,
  AlertTriangle,
  CheckCircle2,
  Edit3,
  Search,
  RefreshCw,
  SlidersHorizontal,
} from "lucide-react";

export default function AdminInventoryPage() {
  const { success, error } = useToast();

  const [branches, setBranches] = useState<Branch[]>([]);
  const [selectedBranchId, setSelectedBranchId] = useState<string>("");
  const [inventoryList, setInventoryList] = useState<BranchInventory[]>([]);
  const [lowStockAlerts, setLowStockAlerts] = useState<BranchInventory[]>([]);
  const [viewMode, setViewMode] = useState<"BRANCH" | "LOW_STOCK">("BRANCH");
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  // Edit stock modal
  const [editingItem, setEditingItem] = useState<BranchInventory | null>(null);
  const [formAvailableQty, setFormAvailableQty] = useState<number>(0);
  const [formSafetyStock, setFormSafetyStock] = useState<number>(10);

  // Load branches
  useEffect(() => {
    const fetchBranches = async () => {
      try {
        const branchList = await api.get<Branch[]>("/api/v1/admin/branches");
        setBranches(branchList);
        if (branchList.length > 0) {
          setSelectedBranchId(branchList[0].id);
        }
      } catch (err) {
        console.warn("Could not load branches", err);
      }
    };
    fetchBranches();
  }, []);

  // Load stock according to viewMode
  const fetchInventory = useCallback(async () => {
    setLoading(true);
    try {
      if (viewMode === "LOW_STOCK") {
        const alerts = await api.get<BranchInventory[]>("/api/v1/admin/inventory/low-stock-alerts");
        setLowStockAlerts(alerts);
      } else if (selectedBranchId) {
        const list = await api.get<BranchInventory[]>(
          `/api/v1/admin/inventory/branches/${selectedBranchId}`
        );
        setInventoryList(list);
      }
    } catch (err) {
      console.warn("Could not load inventory", err);
    } finally {
      setLoading(false);
    }
  }, [viewMode, selectedBranchId]);

  useEffect(() => {
    fetchInventory();
  }, [fetchInventory]);

  // Handle Upsert Stock
  const handleSaveStock = async () => {
    if (!editingItem) return;
    setActionLoading(true);
    try {
      const payload: UpsertBranchInventoryRequest = {
        branchId: editingItem.branchId,
        productId: editingItem.productId,
        availableQty: formAvailableQty,
        safetyStock: formSafetyStock,
      };

      await api.put("/api/v1/admin/inventory", payload);
      success(`Đã cập nhật tồn kho cho sản phẩm ${editingItem.productSku}!`);
      setEditingItem(null);
      fetchInventory();
    } catch (err: any) {
      console.error("Save inventory failed", err);
      error(err.message || "Không thể cập nhật tồn kho");
    } finally {
      setActionLoading(false);
    }
  };

  const currentList = viewMode === "LOW_STOCK" ? lowStockAlerts : inventoryList;

  const filteredItems = currentList.filter((item) => {
    if (!searchTerm.trim()) return true;
    const term = searchTerm.toLowerCase();
    return (
      item.productName.toLowerCase().includes(term) ||
      item.productSku.toLowerCase().includes(term) ||
      item.branchName.toLowerCase().includes(term)
    );
  });

  return (
    <div className="space-y-6">
      {/* Title */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-2.5">
            <Boxes className="w-6 h-6 text-brand-400" />
            Quản Lý Tồn Kho Đa Chi Nhánh (Inventory)
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Theo dõi tồn kho thực tế, số lượng giữ chỗ nguyên tử theo đơn hàng và cảnh báo cạn kho
          </p>
        </div>

        <Button variant="outline" size="sm" onClick={fetchInventory} className="text-xs">
          <RefreshCw className="w-3.5 h-3.5 mr-1" />
          Làm mới
        </Button>
      </div>

      {/* Top Controls Bar */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Toggle between By Branch vs Low Stock Alerts */}
        <div className="flex items-center gap-2 w-full md:w-auto">
          <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-800 text-xs font-semibold">
            <button
              onClick={() => setViewMode("BRANCH")}
              className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                viewMode === "BRANCH"
                  ? "bg-brand-600 text-white"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <Building className="w-3.5 h-3.5" />
              Theo Chi Nhánh
            </button>
            <button
              onClick={() => setViewMode("LOW_STOCK")}
              className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                viewMode === "LOW_STOCK"
                  ? "bg-rose-600 text-white"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <AlertTriangle className="w-3.5 h-3.5" />
              Cảnh Báo Cạn Kho
            </button>
          </div>

          {viewMode === "BRANCH" && (
            <select
              value={selectedBranchId}
              onChange={(e) => setSelectedBranchId(e.target.value)}
              className="bg-slate-950 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.code})
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Search */}
        <div className="w-full md:w-64">
          <Input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo SKU hoặc tên..."
            leftIcon={<Search className="w-4 h-4 text-slate-400" />}
            className="h-9 text-xs bg-slate-950 rounded-xl"
          />
        </div>
      </div>

      {/* Inventory Table */}
      <Card className="border-slate-800 bg-slate-900/60 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-[11px] font-bold uppercase tracking-wider text-slate-400 select-none">
              <tr>
                <th className="p-4">SKU & Sản Phẩm</th>
                <th className="p-4">Kho Chi Nhánh</th>
                <th className="p-4 text-center">Tồn Khả Dụng</th>
                <th className="p-4 text-center">Đang Giữ Chỗ</th>
                <th className="p-4 text-center">Mức An Toàn</th>
                <th className="p-4">Trạng Thái Kho</th>
                <th className="p-4">Cập Nhật</th>
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
              ) : filteredItems.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-12 text-center text-slate-500">
                    <Boxes className="w-8 h-8 mx-auto mb-2 opacity-50" />
                    Không tìm thấy dữ liệu tồn kho phù hợp.
                  </td>
                </tr>
              ) : (
                filteredItems.map((item) => (
                  <tr
                    key={`${item.branchId}-${item.productId}`}
                    className={`hover:bg-slate-800/30 transition-colors ${
                      item.lowStockWarning ? "bg-rose-950/10" : ""
                    }`}
                  >
                    <td className="p-4">
                      <div className="font-bold text-white text-sm">{item.productName}</div>
                      <div className="text-[11px] font-mono text-brand-300 mt-0.5">
                        SKU: {item.productSku}
                      </div>
                    </td>

                    <td className="p-4">
                      <div className="font-medium text-white">{item.branchName}</div>
                      <div className="text-[11px] text-slate-500 font-mono">
                        Mã kho: {item.branchCode}
                      </div>
                    </td>

                    <td className="p-4 text-center">
                      <span className="font-bold text-white text-sm">
                        {item.availableQty}
                      </span>{" "}
                      <span className="text-slate-500 text-[10px]">{item.unitLabel}</span>
                    </td>

                    <td className="p-4 text-center">
                      <span className="font-semibold text-amber-400">
                        {item.reservedQty}
                      </span>{" "}
                      <span className="text-slate-500 text-[10px]">{item.unitLabel}</span>
                    </td>

                    <td className="p-4 text-center">
                      <span className="text-slate-400 font-mono">
                        {item.safetyStock}
                      </span>{" "}
                      <span className="text-slate-500 text-[10px]">{item.unitLabel}</span>
                    </td>

                    <td className="p-4">
                      {item.lowStockWarning ? (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30">
                          <AlertTriangle className="w-3 h-3 text-rose-400" />
                          CẢNH BÁO CẠN KHO
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                          <CheckCircle2 className="w-3 h-3" />
                          Mức An Toàn
                        </span>
                      )}
                    </td>

                    <td className="p-4 text-slate-400 whitespace-nowrap text-[11px]">
                      {formatDate(item.updatedAt)}
                    </td>

                    <td className="p-4 text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setEditingItem(item);
                          setFormAvailableQty(item.availableQty);
                          setFormSafetyStock(item.safetyStock);
                        }}
                        className="text-xs h-7 px-2"
                      >
                        <Edit3 className="w-3.5 h-3.5 mr-1" />
                        Điều chỉnh
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Edit Stock Modal */}
      {editingItem && (
        <Modal
          isOpen={true}
          onClose={() => setEditingItem(null)}
          title={`Điều Chỉnh Tồn Kho: ${editingItem.productName}`}
          maxWidth="sm"
        >
          <div className="space-y-4 py-2 text-xs">
            <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
              <div>Kho: <strong className="text-white">{editingItem.branchName}</strong></div>
              <div>SKU: <span className="font-mono text-brand-300">{editingItem.productSku}</span></div>
              <div>Đang giữ chỗ: <span className="text-amber-400 font-bold">{editingItem.reservedQty} {editingItem.unitLabel}</span></div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">
                Tồn kho khả dụng ({editingItem.unitLabel}) *:
              </label>
              <Input
                type="number"
                min={0}
                value={formAvailableQty}
                onChange={(e) => setFormAvailableQty(Math.max(0, parseInt(e.target.value) || 0))}
                className="bg-slate-950 font-bold"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">
                Mức an toàn tối thiểu ({editingItem.unitLabel}) *:
              </label>
              <Input
                type="number"
                min={0}
                value={formSafetyStock}
                onChange={(e) => setFormSafetyStock(Math.max(0, parseInt(e.target.value) || 0))}
                className="bg-slate-950 font-mono"
              />
              <p className="text-[10px] text-slate-500">
                Hệ thống sẽ gửi cảnh báo khi tồn kho giảm xuống dưới hoặc bằng mức này.
              </p>
            </div>

            <div className="flex justify-end gap-2.5 pt-2 border-t border-slate-800">
              <Button variant="outline" size="sm" onClick={() => setEditingItem(null)}>
                Hủy
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleSaveStock}
                disabled={actionLoading}
                className="text-xs font-bold"
              >
                {actionLoading ? "Đang lưu..." : "Lưu Thay Đổi"}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
