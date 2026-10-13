"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/auth-context";
import { useToast } from "@/context/toast-context";
import { api } from "@/lib/api-client";
import { AuthResponse, B2bOnboardingStatus } from "@/types/auth";
import { B2bOnboardingStepper } from "@/components/ui/stepper";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { CheckCircle2, AlertTriangle, FileEdit, Building, ShieldCheck, ArrowRight, RefreshCw } from "lucide-react";

export default function OnboardingPage() {
  const router = useRouter();
  const { user, isAuthenticated, setAuthSession, refreshProfile } = useAuth();
  const { success, error, info } = useToast();

  const [statusData, setStatusData] = useState<B2bOnboardingStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [showReapplyModal, setShowReapplyModal] = useState(false);

  // Reapply form fields
  const [reapplyForm, setReapplyForm] = useState({
    companyName: "",
    taxCode: "",
    representativeName: "",
    contactPhone: "",
    businessLicenseUrl: "",
    addressLine: "",
  });

  const fetchStatus = async () => {
    try {
      const data = await api.get<B2bOnboardingStatus>("/api/v1/b2b/onboarding/status");
      setStatusData(data);
      setReapplyForm({
        companyName: data.companyName,
        taxCode: data.taxCode,
        representativeName: data.representativeName,
        contactPhone: data.contactPhone,
        businessLicenseUrl: data.businessLicenseUrl,
        addressLine: data.addressLine,
      });
    } catch (err: any) {
      console.warn("Could not fetch onboarding status", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }
    fetchStatus();
  }, [isAuthenticated, router]);

  // Handle Stage 2 Confirmation: Confirm & Activate to ACTIVE
  const handleConfirmOnboarding = async () => {
    setActionLoading(true);
    try {
      const activeTokens = await api.post<AuthResponse>("/api/v1/b2b/onboarding/confirm");
      setAuthSession(activeTokens);
      await refreshProfile();
      success("Chúc mừng! Tài khoản B2B của bạn đã được kích hoạt thành công!");
      router.push("/portal");
    } catch (err: any) {
      error(err.message || "Xác nhận không thành công. Vui lòng thử lại.");
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Stage 1 Re-apply after REJECTED
  const handleReapplySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      const updated = await api.put<B2bOnboardingStatus>("/api/v1/b2b/onboarding/reapply", reapplyForm);
      setStatusData(updated);
      setShowReapplyModal(false);
      success("Hồ sơ đã được gửi lại thành công! Trạng thái chuyển về Giai đoạn 1 (Chờ duyệt).");
    } catch (err: any) {
      error(err.message || "Gửi lại hồ sơ thất bại.");
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 space-y-6">
        <Skeleton className="h-10 w-1/3 mx-auto" />
        <Skeleton className="h-28 w-full rounded-2xl" />
        <Skeleton className="h-64 w-full rounded-2xl" />
      </div>
    );
  }

  if (!statusData) {
    return (
      <div className="max-w-md mx-auto px-4 py-20 text-center space-y-4">
        <AlertTriangle className="w-12 h-12 text-amber-400 mx-auto" />
        <h2 className="text-lg font-bold text-white">Chưa tìm thấy hồ sơ đối tác</h2>
        <p className="text-xs text-slate-400">
          Tài khoản này chưa có hồ sơ B2B nào đang chờ duyệt.
        </p>
        <Button onClick={() => router.push("/register")} variant="primary">
          Đăng ký hồ sơ mới
        </Button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-12 space-y-8">
      {/* Title */}
      <div className="text-center space-y-2">
        <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
          Tiến Trình Xét Duyệt Hồ Sơ B2B
        </h1>
        <p className="text-xs sm:text-sm text-slate-400">
          Theo dõi tiến độ thẩm định pháp nhân của doanh nghiệp <strong>{statusData.companyName}</strong>
        </p>
      </div>

      {/* Stepper Component */}
      <Card className="p-6 sm:p-8 border-slate-800 bg-slate-900/80 backdrop-blur-md">
        <B2bOnboardingStepper
          status={statusData.accountStatus}
          rejectionReason={statusData.rejectionReason}
          approvalNotice={statusData.approvalNotice}
        />
      </Card>

      {/* Summary of Legal Submission */}
      <Card className="p-6 border-slate-800 bg-slate-900/60 backdrop-blur-sm space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <Building className="w-5 h-5 text-brand-400" />
            <h3 className="font-bold text-sm text-white">Chi tiết Hồ sơ Pháp nhân Đã Nộp</h3>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={fetchStatus}
            className="text-xs gap-1 text-slate-400 hover:text-white"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Làm mới</span>
          </Button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div>
            <span className="text-slate-400 block">Tên doanh nghiệp:</span>
            <span className="font-semibold text-white">{statusData.companyName}</span>
          </div>
          <div>
            <span className="text-slate-400 block">Mã số thuế (MST):</span>
            <span className="font-semibold text-white font-mono">{statusData.taxCode}</span>
          </div>
          <div>
            <span className="text-slate-400 block">Người đại diện:</span>
            <span className="font-semibold text-white">{statusData.representativeName}</span>
          </div>
          <div>
            <span className="text-slate-400 block">Số điện thoại:</span>
            <span className="font-semibold text-white">{statusData.contactPhone}</span>
          </div>
          <div className="sm:col-span-2">
            <span className="text-slate-400 block">Địa chỉ kinh doanh:</span>
            <span className="font-semibold text-white">{statusData.addressLine}</span>
          </div>
          <div className="sm:col-span-2">
            <span className="text-slate-400 block">Giấy phép ĐKKD:</span>
            <a
              href={statusData.businessLicenseUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-brand-400 hover:text-brand-300 underline font-medium truncate block max-w-lg mt-0.5"
            >
              {statusData.businessLicenseUrl}
            </a>
          </div>
        </div>
      </Card>

      {/* Action Area based on Current Status */}
      <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-2">
        {statusData.accountStatus === "REJECTED" && (
          <Button
            variant="danger"
            size="lg"
            onClick={() => setShowReapplyModal(true)}
            className="gap-2 font-bold shadow-lg shadow-rose-950"
          >
            <FileEdit className="w-5 h-5" />
            <span>Chỉnh sửa & Nộp lại hồ sơ thẩm định</span>
          </Button>
        )}

        {statusData.accountStatus === "ADMIN_APPROVED" && (
          <Button
            variant="primary"
            size="lg"
            loading={actionLoading}
            onClick={handleConfirmOnboarding}
            className="gap-2 font-bold shadow-lg shadow-brand-600/30"
          >
            <CheckCircle2 className="w-5 h-5" />
            <span>Xác nhận tham gia & Kích hoạt B2B Portal</span>
            <ArrowRight className="w-4 h-4" />
          </Button>
        )}

        {statusData.accountStatus === "ACTIVE" && (
          <Button
            variant="primary"
            size="lg"
            onClick={() => router.push("/portal")}
            className="gap-2 font-bold shadow-lg shadow-emerald-600/30 bg-emerald-600 hover:bg-emerald-500"
          >
            <span>Vào B2B Partner Dashboard</span>
            <ArrowRight className="w-4 h-4" />
          </Button>
        )}
      </div>

      {/* Re-apply Form Modal */}
      {showReapplyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm"
            onClick={() => setShowReapplyModal(false)}
          />
          <div className="relative z-10 w-full max-w-lg rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl space-y-4">
            <h3 className="text-base font-bold text-white">Chỉnh sửa & Nộp lại Hồ sơ Thẩm định</h3>
            <p className="text-xs text-slate-400">
              Vui lòng đính chính các thông tin theo lý do từ chối: "{statusData.rejectionReason}".
            </p>

            <form onSubmit={handleReapplySubmit} className="space-y-3 pt-2">
              <Input
                label="Tên doanh nghiệp"
                value={reapplyForm.companyName}
                onChange={(e) => setReapplyForm({ ...reapplyForm, companyName: e.target.value })}
                required
              />
              <Input
                label="Mã số thuế (MST)"
                value={reapplyForm.taxCode}
                onChange={(e) => setReapplyForm({ ...reapplyForm, taxCode: e.target.value })}
                required
              />
              <Input
                label="Người đại diện"
                value={reapplyForm.representativeName}
                onChange={(e) => setReapplyForm({ ...reapplyForm, representativeName: e.target.value })}
                required
              />
              <Input
                label="Số điện thoại"
                value={reapplyForm.contactPhone}
                onChange={(e) => setReapplyForm({ ...reapplyForm, contactPhone: e.target.value })}
                required
              />
              <Input
                label="Giấy phép ĐKKD (Link)"
                value={reapplyForm.businessLicenseUrl}
                onChange={(e) => setReapplyForm({ ...reapplyForm, businessLicenseUrl: e.target.value })}
                required
              />
              <Input
                label="Địa chỉ trụ sở"
                value={reapplyForm.addressLine}
                onChange={(e) => setReapplyForm({ ...reapplyForm, addressLine: e.target.value })}
                required
              />

              <div className="flex items-center justify-end gap-2 pt-4">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setShowReapplyModal(false)}
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  loading={actionLoading}
                >
                  Gửi lại hồ sơ
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
