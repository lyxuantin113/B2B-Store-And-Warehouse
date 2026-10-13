"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/auth-context";
import { useToast } from "@/context/toast-context";
import { api } from "@/lib/api-client";
import { B2bPartnerProfile } from "@/types/auth";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Tabs } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { formatDate } from "@/lib/utils";
import { Building, Lock, ShieldCheck, UserCheck, MapPin, Phone, Mail, FileText, CheckCircle2 } from "lucide-react";

export default function ProfilePage() {
  const router = useRouter();
  const { user, profile, isAuthenticated, isPartner, logout } = useAuth();
  const { success, error } = useToast();

  const [activeTab, setActiveTab] = useState("profile");
  const [loading, setLoading] = useState(false);
  const [profileData, setProfileData] = useState<B2bPartnerProfile | null>(null);

  // Editable Profile Form
  const [editForm, setEditForm] = useState({
    contactPersonName: "",
    contactPhone: "",
    shippingAddressLine: "",
    shippingAddressDetail: "",
    shippingPostalCode: "700000",
  });

  // Password Form
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  const fetchProfile = async () => {
    try {
      const data = await api.get<B2bPartnerProfile>("/api/v1/b2b/profile");
      setProfileData(data);
      setEditForm({
        contactPersonName: data.contactPersonName || "",
        contactPhone: data.contactPhone || "",
        shippingAddressLine: data.shippingAddressLine || data.addressLine || "",
        shippingAddressDetail: data.shippingAddressDetail || data.addressDetail || "",
        shippingPostalCode: data.shippingPostalCode || data.postalCode || "700000",
      });
    } catch (err: any) {
      console.warn("Could not load profile", err);
    }
  };

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }
    if (isPartner) {
      fetchProfile();
    }
  }, [isAuthenticated, isPartner, router]);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const updated = await api.put<B2bPartnerProfile>("/api/v1/b2b/profile", editForm);
      setProfileData(updated);
      success("Cập nhật thông tin liên hệ và địa chỉ giao hàng thành công!");
    } catch (err: any) {
      error(err.message || "Cập nhật thất bại.");
    } finally {
      setLoading(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      error("Mật khẩu mới xác nhận không khớp.");
      return;
    }
    if (passwordForm.newPassword.length < 8) {
      error("Mật khẩu mới phải có tối thiểu 8 ký tự.");
      return;
    }

    setLoading(true);
    try {
      await api.post("/api/v1/auth/change-password", {
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      success("Đổi mật khẩu thành công! Tất cả các phiên làm việc cũ đã được thu hồi.");
      setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
    } catch (err: any) {
      error(err.message || "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-10 space-y-6">
      {/* Profile Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl border border-slate-800 bg-slate-900/60 backdrop-blur-sm">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-brand-600/20 border border-brand-500/30 flex items-center justify-center text-brand-400 font-black text-xl">
            {user?.fullName?.charAt(0) || "U"}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-black text-white">{profileData?.companyName || user?.fullName}</h1>
              <Badge variant={user?.accountStatus === "ACTIVE" ? "success" : "warning"} size="sm">
                {user?.accountStatus}
              </Badge>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              {user?.email} • Vai trò: <strong className="text-white">{user?.roleCode}</strong>
            </p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <Tabs
        tabs={[
          { id: "profile", label: "Hồ sơ Doanh nghiệp", icon: <Building className="w-4 h-4" /> },
          { id: "security", label: "Đổi Mật khẩu & Bảo mật", icon: <Lock className="w-4 h-4" /> },
        ]}
        activeTab={activeTab}
        onChange={setActiveTab}
      />

      {/* Tab 1: Profile */}
      {activeTab === "profile" && (
        <div className="space-y-6">
          {/* Read-only Approved Legal Info */}
          {profileData && (
            <Card className="p-6 border-slate-800 bg-slate-900/40 space-y-4">
              <div className="flex items-center gap-2 pb-3 border-b border-slate-800 text-xs font-bold text-slate-300 uppercase tracking-wider">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                <span>Thông tin Pháp nhân Đã Duyệt (Chỉ Đọc)</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div>
                  <span className="text-slate-400 block">Tên công ty:</span>
                  <span className="font-semibold text-white">{profileData.companyName}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Mã số thuế:</span>
                  <span className="font-semibold text-white font-mono">{profileData.taxCode}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Loại hình kinh doanh:</span>
                  <span className="font-semibold text-white">{profileData.businessType}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Người đại diện:</span>
                  <span className="font-semibold text-white">{profileData.representativeName}</span>
                </div>
                <div className="sm:col-span-2">
                  <span className="text-slate-400 block">Giấy phép ĐKKD:</span>
                  <a
                    href={profileData.businessLicenseUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-brand-400 hover:text-brand-300 underline font-medium truncate block max-w-lg mt-0.5"
                  >
                    {profileData.businessLicenseUrl}
                  </a>
                </div>
              </div>
            </Card>
          )}

          {/* Editable Operational Contact & Shipping Form */}
          <Card className="p-6 border-slate-800 bg-slate-900/80 backdrop-blur-md space-y-5">
            <div className="pb-3 border-b border-slate-800 text-xs font-bold text-white uppercase tracking-wider">
              Thông tin Người Phụ Trách & Địa Chỉ Nhận Hàng Mặc Định
            </div>

            <form onSubmit={handleUpdateProfile} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Người phụ trách thu mua / sỉ"
                  value={editForm.contactPersonName}
                  onChange={(e) => setEditForm({ ...editForm, contactPersonName: e.target.value })}
                  required
                />
                <Input
                  label="Số điện thoại liên hệ"
                  value={editForm.contactPhone}
                  onChange={(e) => setEditForm({ ...editForm, contactPhone: e.target.value })}
                  required
                />
                <div className="sm:col-span-2">
                  <Input
                    label="Địa chỉ nhận hàng mặc định"
                    value={editForm.shippingAddressLine}
                    onChange={(e) => setEditForm({ ...editForm, shippingAddressLine: e.target.value })}
                    required
                  />
                </div>
                <Input
                  label="Chi tiết địa chỉ (Kho, Tầng...)"
                  value={editForm.shippingAddressDetail}
                  onChange={(e) => setEditForm({ ...editForm, shippingAddressDetail: e.target.value })}
                />
                <Input
                  label="Mã bưu chính"
                  value={editForm.shippingPostalCode}
                  onChange={(e) => setEditForm({ ...editForm, shippingPostalCode: e.target.value })}
                />
              </div>

              <div className="flex justify-end pt-2">
                <Button type="submit" variant="primary" loading={loading} className="px-6">
                  Lưu thay đổi
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* Tab 2: Security & Password */}
      {activeTab === "security" && (
        <Card className="p-6 border-slate-800 bg-slate-900/80 backdrop-blur-md space-y-5">
          <div className="pb-3 border-b border-slate-800 text-xs font-bold text-white uppercase tracking-wider">
            Đổi Mật Khẩu Phiên Làm Việc
          </div>

          <form onSubmit={handleChangePassword} className="space-y-4 max-w-md">
            <Input
              label="Mật khẩu hiện tại"
              type="password"
              value={passwordForm.currentPassword}
              onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
              required
            />
            <Input
              label="Mật khẩu mới (Tối thiểu 8 ký tự)"
              type="password"
              value={passwordForm.newPassword}
              onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
              required
            />
            <Input
              label="Xác nhận mật khẩu mới"
              type="password"
              value={passwordForm.confirmPassword}
              onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
              required
            />

            <div className="pt-2">
              <Button type="submit" variant="primary" loading={loading} className="w-full">
                Cập nhật mật khẩu mới
              </Button>
            </div>
          </form>
        </Card>
      )}
    </div>
  );
}
