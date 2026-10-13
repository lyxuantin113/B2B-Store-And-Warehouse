"use client";

import React, { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api-client";
import { useToast } from "@/context/toast-context";
import { Branch } from "@/types/branch";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Building,
  Plus,
  MapPin,
  Phone,
  Clock,
  Pin,
  CheckCircle2,
  XCircle,
  Edit3,
  Search,
  RefreshCw,
  ExternalLink,
} from "lucide-react";

interface UpsertBranchForm {
  code: string;
  name: string;
  categoryTag: string;
  phone: string;
  postalCode: string;
  addressLine: string;
  addressDetail: string;
  latitude: number;
  longitude: number;
  isPinned: boolean;
  isActive: boolean;
  operatingHours: string;
}

const defaultForm: UpsertBranchForm = {
  code: "",
  name: "",
  categoryTag: "Kho Tổng Phân Phối",
  phone: "1900 6868",
  postalCode: "700000",
  addressLine: "",
  addressDetail: "",
  latitude: 10.7769,
  longitude: 106.7009,
  isPinned: false,
  isActive: true,
  operatingHours: "08:00 - 17:30 (Thứ 2 - Thứ 7)",
};

export default function AdminBranchesPage() {
  const { success, error } = useToast();

  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editingBranch, setEditingBranch] = useState<Branch | null>(null);
  const [form, setForm] = useState<UpsertBranchForm>(defaultForm);
  const [submitting, setSubmitting] = useState(false);

  const fetchBranches = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.get<Branch[]>("/api/v1/admin/branches");
      setBranches(data);
    } catch (err) {
      console.warn("Could not load branches", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBranches();
  }, [fetchBranches]);

  const handleOpenCreate = () => {
    setEditingBranch(null);
    setForm(defaultForm);
    setModalOpen(true);
  };

  const handleOpenEdit = (branch: Branch) => {
    setEditingBranch(branch);
    setForm({
      code: branch.code,
      name: branch.name,
      categoryTag: branch.categoryTag || "Kho Tổng Phân Phối",
      phone: branch.phone || "",
      postalCode: branch.postalCode || "700000",
      addressLine: branch.addressLine,
      addressDetail: branch.addressDetail || "",
      latitude: branch.latitude,
      longitude: branch.longitude,
      isPinned: branch.isPinned ?? false,
      isActive: branch.isActive ?? true,
      operatingHours: branch.operatingHours || "08:00 - 17:30",
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!form.code.trim() || !form.name.trim() || !form.addressLine.trim()) {
      error("Vui lòng điền đầy đủ Mã kho, Tên chi nhánh và Địa chỉ");
      return;
    }

    setSubmitting(true);
    try {
      if (editingBranch) {
        await api.put(`/api/v1/admin/branches/${editingBranch.id}`, form);
        success(`Đã cập nhật chi nhánh ${form.name} thành công!`);
      } else {
        await api.post("/api/v1/admin/branches", form);
        success(`Đã thêm mới chi nhánh ${form.name} thành công!`);
      }
      setModalOpen(false);
      fetchBranches();
    } catch (err: any) {
      console.error("Save branch failed", err);
      error(err.message || "Không thể lưu thông tin chi nhánh");
    } finally {
      setSubmitting(false);
    }
  };

  const filteredBranches = branches.filter((b) => {
    if (!searchTerm.trim()) return true;
    const term = searchTerm.toLowerCase();
    return (
      b.name.toLowerCase().includes(term) ||
      b.code.toLowerCase().includes(term) ||
      b.addressLine.toLowerCase().includes(term)
    );
  });

  return (
    <div className="space-y-6">
      {/* Title */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-2.5">
            <Building className="w-6 h-6 text-brand-400" />
            Hệ Thống Kho & Chi Nhánh (Store Locator CMS)
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Quản lý vị trí địa lý PostGIS, gắn cờ xuất kho cho B2B Wholesale và điểm hiển thị bản đồ công khai
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button variant="primary" size="sm" onClick={handleOpenCreate} className="text-xs font-bold">
            <Plus className="w-3.5 h-3.5 mr-1" />
            Thêm Chi Nhánh / Kho Mới
          </Button>
          <Button variant="outline" size="sm" onClick={fetchBranches} className="text-xs">
            <RefreshCw className="w-3.5 h-3.5 mr-1" />
            Làm mới
          </Button>
        </div>
      </div>

      {/* Filter bar */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 flex items-center justify-between gap-4">
        <div className="text-xs text-slate-300">
          Tổng số: <strong className="text-white">{branches.length}</strong> chi nhánh kho
        </div>
        <div className="w-72">
          <Input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo mã kho, tên, địa chỉ..."
            leftIcon={<Search className="w-4 h-4 text-slate-400" />}
            className="h-9 text-xs bg-slate-950 rounded-xl"
          />
        </div>
      </div>

      {/* Branches Table */}
      <Card className="border-slate-800 bg-slate-900/60 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-[11px] font-bold uppercase tracking-wider text-slate-400 select-none">
              <tr>
                <th className="p-4">Mã Kho & Tên Chi Nhánh</th>
                <th className="p-4">Địa Chỉ & Định Vị PostGIS</th>
                <th className="p-4">Liên Hệ & Giờ Mở Cửa</th>
                <th className="p-4 text-center">Ghim</th>
                <th className="p-4 text-center">Trạng Thái</th>
                <th className="p-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {loading ? (
                [1, 2, 3, 4].map((i) => (
                  <tr key={i}>
                    <td colSpan={6} className="p-4">
                      <Skeleton className="h-8 w-full rounded-xl" />
                    </td>
                  </tr>
                ))
              ) : filteredBranches.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-12 text-center text-slate-500">
                    <Building className="w-8 h-8 mx-auto mb-2 opacity-50" />
                    Không tìm thấy chi nhánh nào phù hợp.
                  </td>
                </tr>
              ) : (
                filteredBranches.map((branch) => (
                  <tr key={branch.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="p-4">
                      <div className="font-bold text-white text-sm flex items-center gap-2">
                        {branch.name}
                        {branch.isPinned && (
                          <span className="p-1 rounded bg-amber-500/20 text-amber-300" title="Chi nhánh nổi bật">
                            <Pin className="w-3 h-3 rotate-45" />
                          </span>
                        )}
                      </div>
                      <div className="text-[11px] font-mono text-brand-300 mt-0.5">
                        MÃ: {branch.code} | {branch.categoryTag || "Kho Tổng"}
                      </div>
                    </td>

                    <td className="p-4 max-w-xs">
                      <div className="text-slate-300 truncate">{branch.addressLine}</div>
                      <div className="text-[11px] text-slate-500 font-mono flex items-center gap-1 mt-0.5">
                        <MapPin className="w-3 h-3 text-slate-400" />
                        {branch.latitude.toFixed(4)}, {branch.longitude.toFixed(4)}
                      </div>
                    </td>

                    <td className="p-4">
                      <div className="text-white flex items-center gap-1">
                        <Phone className="w-3 h-3 text-slate-400" />
                        {branch.phone || "1900 6868"}
                      </div>
                      <div className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                        <Clock className="w-3 h-3 text-slate-500" />
                        {branch.operatingHours || "08:00 - 17:30"}
                      </div>
                    </td>

                    <td className="p-4 text-center">
                      {branch.isPinned ? (
                        <Badge variant="warning" size="sm">Đang Ghim</Badge>
                      ) : (
                        <span className="text-slate-600 text-xs">—</span>
                      )}
                    </td>

                    <td className="p-4 text-center">
                      {branch.isActive ? (
                        <Badge variant="success" size="sm">Hoạt Động</Badge>
                      ) : (
                        <Badge variant="neutral" size="sm">Tạm Ẩn</Badge>
                      )}
                    </td>

                    <td className="p-4 text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleOpenEdit(branch)}
                        className="text-xs h-7 px-2.5"
                      >
                        <Edit3 className="w-3.5 h-3.5 mr-1" />
                        Chỉnh sửa
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Upsert Modal */}
      {modalOpen && (
        <Modal
          isOpen={true}
          onClose={() => setModalOpen(false)}
          title={editingBranch ? `Chỉnh Sửa Kho: ${editingBranch.name}` : "Thêm Chi Nhánh Kho Mới"}
          maxWidth="lg"
        >
          <div className="space-y-4 py-2 text-xs">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Mã kho (Alphanumeric) *:</label>
                <Input
                  value={form.code}
                  onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
                  placeholder="VD: KHO-HANOI-01"
                  className="bg-slate-950 font-mono"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Tên chi nhánh / Kho *:</label>
                <Input
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="VD: Tổng Kho Miền Bắc — Hà Nội"
                  className="bg-slate-950"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Phân loại kho:</label>
                <Input
                  value={form.categoryTag}
                  onChange={(e) => setForm({ ...form, categoryTag: e.target.value })}
                  placeholder="VD: Kho Tổng Phân Phối"
                  className="bg-slate-950"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Số điện thoại liên hệ:</label>
                <Input
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                  placeholder="VD: 024 3829 8899"
                  className="bg-slate-950"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="font-semibold text-slate-300">Địa chỉ chi nhánh *:</label>
              <Input
                value={form.addressLine}
                onChange={(e) => setForm({ ...form, addressLine: e.target.value })}
                placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành..."
                className="bg-slate-950"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Tọa độ Vĩ độ (Latitude) *:</label>
                <Input
                  type="number"
                  step="0.0001"
                  value={form.latitude}
                  onChange={(e) => setForm({ ...form, latitude: parseFloat(e.target.value) || 0 })}
                  className="bg-slate-950 font-mono"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Tọa độ Kinh độ (Longitude) *:</label>
                <Input
                  type="number"
                  step="0.0001"
                  value={form.longitude}
                  onChange={(e) => setForm({ ...form, longitude: parseFloat(e.target.value) || 0 })}
                  className="bg-slate-950 font-mono"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="font-semibold text-slate-300">Khung giờ hoạt động:</label>
              <Input
                value={form.operatingHours}
                onChange={(e) => setForm({ ...form, operatingHours: e.target.value })}
                placeholder="VD: 08:00 - 17:30 (Thứ 2 - Thứ 7)"
                className="bg-slate-950"
              />
            </div>

            <div className="flex items-center gap-6 pt-1">
              <label className="flex items-center gap-2 cursor-pointer text-slate-300">
                <input
                  type="checkbox"
                  checked={form.isPinned}
                  onChange={(e) => setForm({ ...form, isPinned: e.target.checked })}
                  className="w-4 h-4 rounded text-brand-600 bg-slate-950 border-slate-700"
                />
                <span>Ghim ưu tiên trên bản đồ (isPinned)</span>
              </label>

              <label className="flex items-center gap-2 cursor-pointer text-slate-300">
                <input
                  type="checkbox"
                  checked={form.isActive}
                  onChange={(e) => setForm({ ...form, isActive: e.target.checked })}
                  className="w-4 h-4 rounded text-brand-600 bg-slate-950 border-slate-700"
                />
                <span>Kích hoạt xuất hàng (isActive)</span>
              </label>
            </div>

            <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-800">
              <Button variant="outline" size="sm" onClick={() => setModalOpen(false)}>
                Hủy bỏ
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleSubmit}
                disabled={submitting}
                className="text-xs font-bold"
              >
                {submitting ? "Đang lưu..." : editingBranch ? "Cập Nhật Kho" : "Tạo Mới Kho"}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
