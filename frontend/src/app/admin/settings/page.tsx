"use client";

import React, { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api-client";
import { useToast } from "@/context/toast-context";
import { SystemConfigItem, RolePermission, CreateStaffAccountRequest } from "@/types/admin";
import { formatDate } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Tabs } from "@/components/ui/tabs";
import { Modal } from "@/components/ui/modal";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Settings,
  Key,
  ShieldCheck,
  UserPlus,
  Lock,
  Eye,
  EyeOff,
  Save,
  FileText,
  Users,
  CheckCircle2,
  RefreshCw,
  ExternalLink,
} from "lucide-react";

export default function AdminSettingsPage() {
  const { success, error } = useToast();

  const [activeTab, setActiveTab] = useState("configs");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Tab 1: System Configs
  const [configs, setConfigs] = useState<SystemConfigItem[]>([]);
  const [editingConfig, setEditingConfig] = useState<SystemConfigItem | null>(null);
  const [configValueInput, setConfigValueInput] = useState("");

  // Tab 2: Policy CMS
  const [policyContent, setPolicyContent] = useState("");
  const [policyUpdatedAt, setPolicyUpdatedAt] = useState<string | null>(null);

  // Tab 3: RBAC & Staff Accounts
  const [roles, setRoles] = useState<RolePermission[]>([]);
  const [createStaffModalOpen, setCreateStaffModalOpen] = useState(false);
  const [staffForm, setStaffForm] = useState<CreateStaffAccountRequest>({
    email: "",
    password: "",
    fullName: "",
    phone: "",
    roleCode: "SELLER",
  });

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [configList, policyRes, roleList] = await Promise.all([
        api.get<SystemConfigItem[]>("/api/v1/admin/system-configs"),
        api.get<{ configKey: string; contentHtml: string; updatedAt?: string }>("/api/v1/public/policy", {
          requiresAuth: false,
        }),
        api.get<RolePermission[]>("/api/v1/admin/rbac/roles"),
      ]);
      setConfigs(configList);
      setPolicyContent(policyRes.contentHtml || "");
      setPolicyUpdatedAt(policyRes.updatedAt || null);
      setRoles(roleList);
    } catch (err) {
      console.warn("Could not load admin settings", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Save single system config
  const handleSaveConfig = async () => {
    if (!editingConfig) return;
    setSaving(true);
    try {
      await api.put("/api/v1/admin/system-configs", {
        configKey: editingConfig.configKey,
        configGroup: editingConfig.configGroup,
        configValue: configValueInput,
        isEncrypted: editingConfig.isEncrypted,
      });
      success(`Đã cập nhật cấu hình ${editingConfig.configKey}!`);
      setEditingConfig(null);
      fetchData();
    } catch (err: any) {
      error(err.message || "Không thể lưu cấu hình");
    } finally {
      setSaving(false);
    }
  };

  // Save Policy HTML
  const handleSavePolicy = async () => {
    setSaving(true);
    try {
      await api.put("/api/v1/admin/system-configs/policy", {
        contentHtml: policyContent,
      });
      success("Đã cập nhật toàn văn Chính sách B2B thành công!");
      fetchData();
    } catch (err: any) {
      error(err.message || "Không thể lưu chính sách");
    } finally {
      setSaving(false);
    }
  };

  // Create Staff Account
  const handleCreateStaff = async () => {
    if (!staffForm.email || !staffForm.password || !staffForm.fullName) {
      error("Vui lòng điền đầy đủ Email, Mật khẩu và Họ tên");
      return;
    }
    setSaving(true);
    try {
      await api.post("/api/v1/admin/rbac/staff-accounts", staffForm);
      success(`Đã tạo tài khoản nhân sự ${staffForm.fullName} (${staffForm.roleCode}) thành công!`);
      setCreateStaffModalOpen(false);
      setStaffForm({ email: "", password: "", fullName: "", phone: "", roleCode: "SELLER" });
    } catch (err: any) {
      error(err.message || "Không thể tạo tài khoản nhân sự");
    } finally {
      setSaving(false);
    }
  };

  const settingsTabs = [
    { id: "configs", label: "Khóa API & Cấu Hình Cổng", icon: <Key className="w-4 h-4" /> },
    { id: "policy", label: "Quản Lý Chính Sách B2B", icon: <FileText className="w-4 h-4" /> },
    { id: "rbac", label: "Tài Khoản Nhân Sự & Phân Quyền", icon: <Users className="w-4 h-4" /> },
  ];

  return (
    <div className="space-y-6">
      {/* Title */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-2.5">
            <Settings className="w-6 h-6 text-brand-400" />
            Cấu Hình Hệ Thống, Chính Sách & Phân Quyền
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Bảo mật AES-256-GCM các khóa bí mật cổng thanh toán, đơn vị giao vận, CMS văn bản và cấp tài khoản nội bộ
          </p>
        </div>

        <Button variant="outline" size="sm" onClick={fetchData} className="text-xs">
          <RefreshCw className="w-3.5 h-3.5 mr-1" />
          Làm mới
        </Button>
      </div>

      {/* Tabs */}
      <Tabs tabs={settingsTabs} activeTab={activeTab} onChange={setActiveTab} />

      {/* Tab 1: System Configs */}
      {activeTab === "configs" && (
        <div className="space-y-4">
          <div className="p-4 rounded-2xl border border-brand-500/20 bg-brand-950/20 flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-brand-400 shrink-0 mt-0.5" />
            <div className="text-xs text-slate-300 leading-relaxed">
              Các khóa API bí mật của <strong>VNPay, MoMo, VietQR, GHN, AhaMove và Google Maps</strong> được mã hóa bảo vệ bằng thuật toán <strong>AES-256-GCM</strong> trong cơ sở dữ liệu. Dữ liệu nhạy cảm được che giấu tự động khi xem.
            </div>
          </div>

          <Card className="border-slate-800 bg-slate-900/60 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/80 border-b border-slate-800 text-[11px] font-bold uppercase tracking-wider text-slate-400 select-none">
                  <tr>
                    <th className="p-4">Khóa Cấu Hình (Config Key)</th>
                    <th className="p-4">Nhóm Cổng</th>
                    <th className="p-4">Giá Trị Hiện Tại</th>
                    <th className="p-4 text-center">Bảo Mật</th>
                    <th className="p-4">Cập Nhật</th>
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
                  ) : configs.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="p-12 text-center text-slate-500">
                        Chưa có cấu hình hệ thống nào.
                      </td>
                    </tr>
                  ) : (
                    configs.map((cfg) => (
                      <tr key={cfg.configKey} className="hover:bg-slate-800/30 transition-colors">
                        <td className="p-4 font-mono font-bold text-white text-xs">
                          {cfg.configKey}
                        </td>
                        <td className="p-4">
                          <span className="px-2 py-0.5 rounded bg-slate-800 text-brand-300 font-semibold text-[10px]">
                            {cfg.configGroup}
                          </span>
                        </td>
                        <td className="p-4 font-mono text-slate-400 max-w-xs truncate">
                          {cfg.configValue}
                        </td>
                        <td className="p-4 text-center">
                          {cfg.isEncrypted ? (
                            <Badge variant="brand" size="sm">AES-256-GCM</Badge>
                          ) : (
                            <span className="text-slate-500 text-[11px]">PlainText</span>
                          )}
                        </td>
                        <td className="p-4 text-slate-400 whitespace-nowrap text-[11px]">
                          {formatDate(cfg.updatedAt)}
                        </td>
                        <td className="p-4 text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setEditingConfig(cfg);
                              setConfigValueInput(cfg.isEncrypted ? "" : cfg.configValue);
                            }}
                            className="text-xs h-7 px-2.5"
                          >
                            Cập nhật
                          </Button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      )}

      {/* Tab 2: Policy CMS */}
      {activeTab === "policy" && (
        <Card className="p-6 border-slate-800 bg-slate-900/60 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div>
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <FileText className="w-4 h-4 text-brand-400" />
                Văn Bản Chính Sách Đối Tác Doanh Nghiệp B2B
              </h3>
              <p className="text-xs text-slate-400 mt-0.5">
                Nội dung HTML hiển thị tại trang công khai `/policy` và Tab Chính sách trong B2B Portal
              </p>
            </div>
            {policyUpdatedAt && (
              <span className="text-xs text-slate-500">
                Cập nhật lần cuối: {formatDate(policyUpdatedAt)}
              </span>
            )}
          </div>

          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-300">
              Nội dung chính sách (HTML/Rich-Text):
            </label>
            <textarea
              value={policyContent}
              onChange={(e) => setPolicyContent(e.target.value)}
              rows={16}
              className="w-full bg-slate-950 border border-slate-700 rounded-2xl p-4 text-xs font-mono text-slate-200 focus:outline-none focus:border-brand-500 leading-relaxed"
            />
          </div>

          <div className="flex justify-end pt-2">
            <Button
              variant="primary"
              size="md"
              onClick={handleSavePolicy}
              disabled={saving}
              className="text-xs font-bold"
            >
              <Save className="w-4 h-4 mr-1.5" />
              {saving ? "Đang lưu..." : "Lưu Thay Đổi Chính Sách"}
            </Button>
          </div>
        </Card>
      )}

      {/* Tab 3: Staff Accounts & RBAC */}
      {activeTab === "rbac" && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <h3 className="text-sm font-bold text-white">Ma Trận Phân Quyền Vai Trò (RBAC Matrix)</h3>
              <p className="text-xs text-slate-400">
                Các quyền hạn được bảo vệ nghiêm ngặt bằng Spring Security 6 Method Security
              </p>
            </div>

            <Button
              variant="primary"
              size="sm"
              onClick={() => setCreateStaffModalOpen(true)}
              className="text-xs font-bold"
            >
              <UserPlus className="w-3.5 h-3.5 mr-1.5" />
              Tạo Tài Khoản Nhân Sự Mới
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {roles.map((r) => (
              <Card key={r.roleCode} className="p-5 border-slate-800 bg-slate-900/70 space-y-3">
                <div className="flex items-center justify-between border-b border-slate-800 pb-2.5">
                  <span className="font-bold text-white text-sm">{r.roleName}</span>
                  <Badge
                    variant={r.roleCode === "ADMIN" ? "danger" : r.roleCode === "MANAGER" ? "warning" : "info"}
                    size="sm"
                  >
                    {r.roleCode}
                  </Badge>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">{r.description}</p>
                <div className="space-y-1 pt-1">
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
                    Quyền hạn cấp phát ({r.permissions.length}):
                  </span>
                  <div className="flex flex-wrap gap-1 max-h-36 overflow-y-auto pt-1">
                    {r.permissions.map((perm) => (
                      <span
                        key={perm}
                        className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-950 text-slate-300 border border-slate-800"
                      >
                        {perm}
                      </span>
                    ))}
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Edit Config Modal */}
      {editingConfig && (
        <Modal
          isOpen={true}
          onClose={() => setEditingConfig(null)}
          title={`Cập Nhật Cấu Hình: ${editingConfig.configKey}`}
          maxWidth="md"
        >
          <div className="space-y-4 py-2 text-xs">
            <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
              <div>Khóa: <strong className="text-white font-mono">{editingConfig.configKey}</strong></div>
              <div>Nhóm: <span className="text-brand-300 font-semibold">{editingConfig.configGroup}</span></div>
              {editingConfig.isEncrypted && (
                <div className="text-emerald-400 text-[11px] font-semibold">
                  ★ Dữ liệu sẽ được mã hóa AES-256-GCM tự động khi lưu.
                </div>
              )}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">
                Giá trị cấu hình mới *:
              </label>
              <Input
                value={configValueInput}
                onChange={(e) => setConfigValueInput(e.target.value)}
                placeholder={editingConfig.isEncrypted ? "Nhập khóa bí mật mới để ghi đè..." : "Nhập giá trị cấu hình..."}
                className="bg-slate-950 font-mono"
              />
            </div>

            <div className="flex justify-end gap-2.5 pt-2 border-t border-slate-800">
              <Button variant="outline" size="sm" onClick={() => setEditingConfig(null)}>
                Hủy
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleSaveConfig}
                disabled={saving || !configValueInput.trim()}
                className="text-xs font-bold"
              >
                {saving ? "Đang lưu..." : "Lưu Cấu Hình"}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Create Staff Account Modal */}
      {createStaffModalOpen && (
        <Modal
          isOpen={true}
          onClose={() => setCreateStaffModalOpen(false)}
          title="Tạo Tài Khoản Nhân Viên Nội Bộ (Staff Provisioning)"
          maxWidth="md"
        >
          <div className="space-y-3.5 py-2 text-xs">
            <div className="space-y-1">
              <label className="font-semibold text-slate-300">Họ và tên nhân sự *:</label>
              <Input
                value={staffForm.fullName}
                onChange={(e) => setStaffForm({ ...staffForm, fullName: e.target.value })}
                placeholder="VD: Nguyễn Văn Quản Lý"
                className="bg-slate-950"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Email đăng nhập *:</label>
                <Input
                  type="email"
                  value={staffForm.email}
                  onChange={(e) => setStaffForm({ ...staffForm, email: e.target.value })}
                  placeholder="manager@umego.vn"
                  className="bg-slate-950"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Số điện thoại:</label>
                <Input
                  value={staffForm.phone}
                  onChange={(e) => setStaffForm({ ...staffForm, phone: e.target.value })}
                  placeholder="0909123456"
                  className="bg-slate-950"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Mật khẩu khởi tạo *:</label>
                <Input
                  type="password"
                  value={staffForm.password}
                  onChange={(e) => setStaffForm({ ...staffForm, password: e.target.value })}
                  placeholder="••••••••"
                  className="bg-slate-950"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-slate-300">Vai trò đảm nhiệm *:</label>
                <select
                  value={staffForm.roleCode}
                  onChange={(e) =>
                    setStaffForm({
                      ...staffForm,
                      roleCode: e.target.value as "ADMIN" | "MANAGER" | "SELLER",
                    })
                  }
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 h-9 text-xs text-white focus:outline-none"
                >
                  <option value="SELLER">SELLER (Nhân viên bán hàng)</option>
                  <option value="MANAGER">MANAGER (Quản lý kho/chi nhánh)</option>
                  <option value="ADMIN">ADMIN (Quản trị viên toàn hệ thống)</option>
                </select>
              </div>
            </div>

            <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-800">
              <Button variant="outline" size="sm" onClick={() => setCreateStaffModalOpen(false)}>
                Hủy bỏ
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleCreateStaff}
                disabled={saving}
                className="text-xs font-bold"
              >
                {saving ? "Đang tạo..." : "Khởi Tạo Tài Khoản"}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
