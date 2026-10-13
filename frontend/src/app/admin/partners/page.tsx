"use client";

import React, { Suspense, useEffect, useState, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { api } from "@/lib/api-client";
import { useToast } from "@/context/toast-context";
import { B2bPartnerProfile, AccountStatus } from "@/types/auth";
import { AdminPartnerKpi, AdminPartnerStatusUpdateRequest, AdminBulkPartnerStatusRequest } from "@/types/admin";
import { formatDate } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Users,
  Search,
  CheckCircle2,
  XCircle,
  Clock,
  ShieldAlert,
  FileText,
  ExternalLink,
  Filter,
  CheckSquare,
  Square,
  Building,
  Phone,
  Mail,
  UserCheck,
  AlertCircle,
  RefreshCw,
} from "lucide-react";

function PartnersCrmContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { success, error } = useToast();

  const initialStatus = (searchParams.get("status") as AccountStatus) || "";
  const [selectedStatus, setSelectedStatus] = useState<string>(initialStatus);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [partners, setPartners] = useState<B2bPartnerProfile[]>([]);
  const [kpis, setKpis] = useState<AdminPartnerKpi | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  // Selection for bulk actions
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  // Modals state
  const [activePartner, setActivePartner] = useState<B2bPartnerProfile | null>(null);
  const [modalMode, setModalMode] = useState<"APPROVE" | "REJECT" | "SUSPEND" | "VIEW" | null>(null);
  const [approvalNotice, setApprovalNotice] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const statusQuery = selectedStatus ? `status=${selectedStatus}&` : "";
      const keywordQuery = searchKeyword ? `keyword=${encodeURIComponent(searchKeyword)}&` : "";
      const [kpiRes, partnerRes] = await Promise.all([
        api.get<AdminPartnerKpi>("/api/v1/admin/partners/kpis"),
        api.get<B2bPartnerProfile[]>(`/api/v1/admin/partners?${statusQuery}${keywordQuery}page=0&size=50`),
      ]);
      setKpis(kpiRes);
      setPartners(partnerRes);
      setSelectedIds([]);
    } catch (err) {
      console.warn("Could not load partner CRM data", err);
    } finally {
      setLoading(false);
    }
  }, [selectedStatus, searchKeyword]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Bulk selection toggles
  const handleToggleSelect = (id: string) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleSelectAll = () => {
    if (selectedIds.length === partners.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(partners.map((p) => p.partnerId || p.id || p.userId));
    }
  };

  // Status transition handlers
  const handleUpdateStatus = async (partner: B2bPartnerProfile, targetStatus: AccountStatus) => {
    const pId = partner.partnerId || partner.id;
    if (!pId) return;

    setActionLoading(true);
    try {
      const payload: AdminPartnerStatusUpdateRequest = {
        targetStatus,
        approvalNotice: targetStatus === "ADMIN_APPROVED" ? approvalNotice : undefined,
        rejectionReason: targetStatus === "REJECTED" ? rejectionReason : undefined,
      };

      await api.patch(`/api/v1/admin/partners/${pId}/status`, payload);
      success(`Đã cập nhật trạng thái đối tác thành ${targetStatus}`);
      setModalMode(null);
      setActivePartner(null);
      setApprovalNotice("");
      setRejectionReason("");
      fetchData();
    } catch (err: any) {
      console.error("Update partner status failed", err);
      error(err.message || "Không thể cập nhật trạng thái");
    } finally {
      setActionLoading(false);
    }
  };

  // Bulk status update
  const handleBulkStatus = async (targetStatus: AccountStatus) => {
    if (selectedIds.length === 0) return;
    setActionLoading(true);
    try {
      const payload: AdminBulkPartnerStatusRequest = {
        partnerIds: selectedIds,
        targetStatus,
        approvalNotice: targetStatus === "ADMIN_APPROVED" ? "Hồ sơ đã được duyệt hàng loạt bởi Admin." : undefined,
      };
      await api.post("/api/v1/admin/partners/bulk-status", payload);
      success(`Đã cập nhật hàng loạt ${selectedIds.length} đối tác sang ${targetStatus}`);
      setSelectedIds([]);
      fetchData();
    } catch (err: any) {
      console.error("Bulk status update failed", err);
      error(err.message || "Thao tác hàng loạt thất bại");
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusBadge = (status: AccountStatus) => {
    switch (status) {
      case "PENDING_ADMIN":
        return <Badge variant="warning" size="sm">Chờ Thẩm Định</Badge>;
      case "ADMIN_APPROVED":
        return <Badge variant="info" size="sm">Chờ Xác Nhận</Badge>;
      case "ACTIVE":
        return <Badge variant="success" size="sm">Đang Hoạt Động</Badge>;
      case "REJECTED":
        return <Badge variant="danger" size="sm">Từ Chối</Badge>;
      case "SUSPENDED":
      case "INACTIVE":
        return <Badge variant="neutral" size="sm">Tạm Khóa</Badge>;
      default:
        return <Badge variant="neutral" size="sm">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Title */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-2.5">
            <Users className="w-6 h-6 text-brand-400" />
            Quản Lý Đối Tác B2B (Partner CRM)
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Quy trình thẩm định pháp lý 3 giai đoạn, phê duyệt hồ sơ ĐKKD và cấp hạn mức sỉ
          </p>
        </div>

        <Button variant="outline" size="sm" onClick={fetchData} className="text-xs">
          <RefreshCw className="w-3.5 h-3.5 mr-1.5" />
          Làm mới dữ liệu
        </Button>
      </div>

      {/* 4 KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <Card className="p-4 border-slate-800 bg-slate-900/60 space-y-1">
          <div className="text-xs text-slate-400">Tổng Đối Tác Đăng Ký</div>
          <div className="text-2xl font-black text-white">{kpis?.totalPartners || 0}</div>
          <div className="text-[11px] text-slate-400">Toàn bộ hồ sơ doanh nghiệp</div>
        </Card>

        <Card className="p-4 border-amber-500/30 bg-amber-950/20 space-y-1">
          <div className="text-xs text-amber-400 font-semibold">Chờ Thẩm Định (Stage 1)</div>
          <div className="text-2xl font-black text-amber-300">{kpis?.pendingReviewCount || 0}</div>
          <div className="text-[11px] text-amber-400/80">Cần kiểm tra giấy phép ĐKKD</div>
        </Card>

        <Card className="p-4 border-sky-500/30 bg-sky-950/20 space-y-1">
          <div className="text-xs text-sky-400 font-semibold">Chờ Xác Nhận (Stage 2)</div>
          <div className="text-2xl font-black text-sky-300">{kpis?.awaitingConfirmationCount || 0}</div>
          <div className="text-[11px] text-sky-400/80">Admin đã duyệt sơ bộ</div>
        </Card>

        <Card className="p-4 border-emerald-500/30 bg-emerald-950/20 space-y-1">
          <div className="text-xs text-emerald-400 font-semibold">Chính Thức Hoạt Động (Stage 3)</div>
          <div className="text-2xl font-black text-emerald-300">{kpis?.activeCount || 0}</div>
          <div className="text-[11px] text-emerald-400/80">Đã mở khóa đặt hàng sỉ & mẫu</div>
        </Card>
      </div>

      {/* Filter Tabs & Search Bar */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Status filter buttons */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto no-scrollbar">
          {[
            { id: "", label: "Tất cả" },
            { id: "PENDING_ADMIN", label: "Chờ thẩm định" },
            { id: "ADMIN_APPROVED", label: "Chờ xác nhận" },
            { id: "ACTIVE", label: "Đang hoạt động" },
            { id: "REJECTED", label: "Bị từ chối" },
            { id: "SUSPENDED", label: "Tạm khóa" },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setSelectedStatus(tab.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors ${
                selectedStatus === tab.id
                  ? "bg-brand-600 text-white shadow-sm"
                  : "bg-slate-950 text-slate-400 hover:text-white border border-slate-800"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="w-full md:w-72">
          <Input
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            placeholder="Tìm theo MST, tên công ty, email..."
            leftIcon={<Search className="w-4 h-4 text-slate-400" />}
            className="h-9 text-xs bg-slate-950 rounded-xl"
          />
        </div>
      </div>

      {/* Bulk Action Bar (when rows selected) */}
      {selectedIds.length > 0 && (
        <div className="p-3.5 rounded-2xl bg-brand-950/40 border border-brand-500/40 flex items-center justify-between gap-4 text-xs">
          <div className="flex items-center gap-2 text-white font-semibold">
            <CheckSquare className="w-4 h-4 text-brand-400" />
            Đã chọn {selectedIds.length} đối tác
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleBulkStatus("ADMIN_APPROVED")}
              disabled={actionLoading}
              className="text-xs h-7 border-emerald-500/30 text-emerald-300"
            >
              Duyệt sơ bộ hàng loạt
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleBulkStatus("ACTIVE")}
              disabled={actionLoading}
              className="text-xs h-7 border-brand-500/30 text-brand-300"
            >
              Kích hoạt hàng loạt
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setSelectedIds([])}
              className="text-xs h-7 text-slate-400"
            >
              Bỏ chọn
            </Button>
          </div>
        </div>
      )}

      {/* Partners Table */}
      <Card className="border-slate-800 bg-slate-900/60 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-[11px] font-bold uppercase tracking-wider text-slate-400 select-none">
              <tr>
                <th className="p-4 w-10">
                  <button onClick={handleSelectAll} className="p-0.5">
                    {selectedIds.length > 0 && selectedIds.length === partners.length ? (
                      <CheckSquare className="w-4 h-4 text-brand-400" />
                    ) : (
                      <Square className="w-4 h-4 text-slate-600" />
                    )}
                  </button>
                </th>
                <th className="p-4">Doanh Nghiệp & MST</th>
                <th className="p-4">Người Đại Diện & Liên Hệ</th>
                <th className="p-4">Hồ Sơ ĐKKD</th>
                <th className="p-4">Trạng Thái</th>
                <th className="p-4">Ngày Đăng Ký</th>
                <th className="p-4 text-right">Thao Tác Thẩm Định</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {loading ? (
                [1, 2, 3, 4, 5].map((i) => (
                  <tr key={i}>
                    <td colSpan={7} className="p-4">
                      <Skeleton className="h-8 w-full rounded-xl" />
                    </td>
                  </tr>
                ))
              ) : partners.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-12 text-center text-slate-500">
                    <Users className="w-8 h-8 mx-auto mb-2 opacity-50" />
                    Không tìm thấy hồ sơ đối tác nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                partners.map((partner) => {
                  const pId = partner.partnerId || partner.id || partner.userId;
                  const isSelected = selectedIds.includes(pId);

                  return (
                    <tr
                      key={pId}
                      className={`hover:bg-slate-800/30 transition-colors ${
                        isSelected ? "bg-brand-500/5" : ""
                      }`}
                    >
                      <td className="p-4">
                        <button onClick={() => handleToggleSelect(pId)} className="p-0.5">
                          {isSelected ? (
                            <CheckSquare className="w-4 h-4 text-brand-400" />
                          ) : (
                            <Square className="w-4 h-4 text-slate-600" />
                          )}
                        </button>
                      </td>

                      <td className="p-4">
                        <div className="font-bold text-white text-sm">{partner.companyName}</div>
                        <div className="text-slate-400 text-[11px] font-mono flex items-center gap-1.5 mt-0.5">
                          <span>MST: {partner.taxCode}</span>
                          <span className="text-slate-600">•</span>
                          <span>{partner.businessType || "Thương mại sỉ"}</span>
                        </div>
                      </td>

                      <td className="p-4">
                        <div className="text-white font-medium">
                          {partner.representativeName || partner.contactPersonName}
                        </div>
                        <div className="text-slate-400 text-[11px] flex items-center gap-2 mt-0.5">
                          <span className="flex items-center gap-1">
                            <Phone className="w-3 h-3 text-slate-500" />
                            {partner.contactPhone}
                          </span>
                          <span className="flex items-center gap-1">
                            <Mail className="w-3 h-3 text-slate-500" />
                            {partner.email}
                          </span>
                        </div>
                      </td>

                      <td className="p-4">
                        {partner.businessLicenseUrl ? (
                          <a
                            href={partner.businessLicenseUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-800 text-brand-300 hover:text-white border border-slate-700 transition-colors font-semibold"
                          >
                            <FileText className="w-3.5 h-3.5 text-brand-400" />
                            Xem file GPKD
                            <ExternalLink className="w-3 h-3" />
                          </a>
                        ) : (
                          <span className="text-slate-500 italic">Chưa tải file</span>
                        )}
                        {partner.reapplyCount != null && partner.reapplyCount > 0 && (
                          <div className="text-[10px] text-amber-400 mt-1">
                            Nộp lại lần {partner.reapplyCount}
                          </div>
                        )}
                      </td>

                      <td className="p-4">{getStatusBadge(partner.accountStatus)}</td>

                      <td className="p-4 text-slate-400 whitespace-nowrap">
                        {formatDate(partner.createdAt)}
                      </td>

                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Approve Button */}
                          {partner.accountStatus === "PENDING_ADMIN" && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setActivePartner(partner);
                                setModalMode("APPROVE");
                                setApprovalNotice("Hồ sơ ĐKKD đã hợp lệ. Vui lòng kiểm tra lại thông tin và bấm Hoàn tất gia nhập.");
                              }}
                              className="text-xs h-7 px-2.5 text-emerald-400 border-emerald-500/40 hover:bg-emerald-500/10"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                              Duyệt GPKD
                            </Button>
                          )}

                          {/* Direct Activate */}
                          {partner.accountStatus === "ADMIN_APPROVED" && (
                            <Button
                              variant="primary"
                              size="sm"
                              onClick={() => handleUpdateStatus(partner, "ACTIVE")}
                              className="text-xs h-7 px-2.5 font-bold"
                            >
                              Kích hoạt ngay
                            </Button>
                          )}

                          {/* Reject Button */}
                          {(partner.accountStatus === "PENDING_ADMIN" || partner.accountStatus === "ADMIN_APPROVED") && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setActivePartner(partner);
                                setModalMode("REJECT");
                                setRejectionReason("");
                              }}
                              className="text-xs h-7 px-2 text-rose-400 border-rose-500/30 hover:bg-rose-500/10"
                            >
                              <XCircle className="w-3.5 h-3.5 mr-1" />
                              Từ chối
                            </Button>
                          )}

                          {/* Suspend Button */}
                          {partner.accountStatus === "ACTIVE" && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setActivePartner(partner);
                                setModalMode("SUSPEND");
                              }}
                              className="text-xs h-7 px-2 text-slate-400 hover:text-white"
                            >
                              Khóa
                            </Button>
                          )}

                          {/* View details */}
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              setActivePartner(partner);
                              setModalMode("VIEW");
                            }}
                            className="text-xs h-7 px-2 text-slate-400 hover:text-white"
                          >
                            Chi tiết
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Approval Modal */}
      {modalMode === "APPROVE" && activePartner && (
        <Modal
          isOpen={true}
          onClose={() => setModalMode(null)}
          title={`Phê Duyệt Sơ Bộ Hồ Sơ B2B: ${activePartner.companyName}`}
          maxWidth="md"
        >
          <div className="space-y-4 py-2">
            <p className="text-xs text-slate-300 leading-relaxed">
              Doanh nghiệp sẽ được chuyển sang trạng thái <strong>ADMIN_APPROVED (Stage 2)</strong>. Khi đăng nhập, đối tác sẽ nhận được hướng dẫn và xác nhận thông tin pháp nhân lần cuối để kích hoạt.
            </p>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">
                Lời nhắn hướng dẫn đối tác (Approval Notice):
              </label>
              <textarea
                value={approvalNotice}
                onChange={(e) => setApprovalNotice(e.target.value)}
                rows={3}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-brand-500"
              />
            </div>

            <div className="flex justify-end gap-2.5 pt-2">
              <Button variant="outline" size="sm" onClick={() => setModalMode(null)}>
                Hủy bỏ
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={() => handleUpdateStatus(activePartner, "ADMIN_APPROVED")}
                disabled={actionLoading}
                className="text-xs font-bold"
              >
                {actionLoading ? "Đang lưu..." : "Xác Nhận Phê Duyệt"}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Rejection Modal */}
      {modalMode === "REJECT" && activePartner && (
        <Modal
          isOpen={true}
          onClose={() => setModalMode(null)}
          title={`Từ Chối Hồ Sơ B2B: ${activePartner.companyName}`}
          maxWidth="md"
        >
          <div className="space-y-4 py-2">
            <p className="text-xs text-rose-300 leading-relaxed">
              Vui lòng nêu rõ lý do từ chối để doanh nghiệp có thể chỉnh sửa lại hồ sơ và nộp lại (Re-apply) trực tiếp trên tài khoản hiện tại.
            </p>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">
                Lý do từ chối (Rejection Reason) *:
              </label>
              <textarea
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                placeholder="Ví dụ: Giấy phép ĐKKD mờ không đọc được mã số thuế, vui lòng chụp lại bản gốc..."
                rows={3}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-rose-500"
              />
            </div>

            <div className="flex justify-end gap-2.5 pt-2">
              <Button variant="outline" size="sm" onClick={() => setModalMode(null)}>
                Hủy bỏ
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleUpdateStatus(activePartner, "REJECTED")}
                disabled={actionLoading || !rejectionReason.trim()}
                className="text-xs text-rose-400 border-rose-500/40 hover:bg-rose-500/10 font-bold"
              >
                {actionLoading ? "Đang lưu..." : "Xác Nhận Từ Chối"}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Suspend Confirmation Modal */}
      {modalMode === "SUSPEND" && activePartner && (
        <Modal
          isOpen={true}
          onClose={() => setModalMode(null)}
          title={`Tạm Khóa Đối Tác: ${activePartner.companyName}`}
          maxWidth="sm"
        >
          <div className="space-y-4 py-2">
            <p className="text-xs text-slate-300">
              Đối tác sẽ bị tạm ngưng quyền đặt hàng sỉ và gửi yêu cầu mẫu thử. Bạn có thể mở khóa lại bất kỳ lúc nào.
            </p>
            <div className="flex justify-end gap-2.5">
              <Button variant="outline" size="sm" onClick={() => setModalMode(null)}>
                Hủy
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleUpdateStatus(activePartner, "SUSPENDED")}
                disabled={actionLoading}
                className="text-xs text-rose-400 border-rose-500/40 font-bold"
              >
                Xác Nhận Tạm Khóa
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* View Details Modal */}
      {modalMode === "VIEW" && activePartner && (
        <Modal
          isOpen={true}
          onClose={() => setModalMode(null)}
          title={`Hồ Sơ Chi Tiết: ${activePartner.companyName}`}
          maxWidth="lg"
        >
          <div className="space-y-4 py-2 text-xs">
            <div className="grid grid-cols-2 gap-4">
              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  Pháp Lý Doanh Nghiệp
                </span>
                <div className="space-y-1.5">
                  <div>Tên công ty: <strong className="text-white">{activePartner.companyName}</strong></div>
                  <div>Mã số thuế: <strong className="text-brand-300 font-mono">{activePartner.taxCode}</strong></div>
                  <div>Loại hình: <span className="text-slate-300">{activePartner.businessType}</span></div>
                  <div>Địa chỉ ĐKKD: <span className="text-slate-300">{activePartner.addressLine}</span></div>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  Đại Diện & Giao Nhận
                </span>
                <div className="space-y-1.5">
                  <div>Người đại diện: <span className="text-white">{activePartner.representativeName}</span></div>
                  <div>Người liên hệ: <span className="text-white">{activePartner.contactPersonName}</span></div>
                  <div>SĐT: <span className="text-white font-mono">{activePartner.contactPhone}</span></div>
                  <div>Email: <span className="text-white">{activePartner.email}</span></div>
                </div>
              </div>
            </div>

            {activePartner.rejectionReason && (
              <div className="p-3 rounded-xl bg-rose-950/30 border border-rose-500/30 text-rose-300">
                <strong>Lý do từ chối trước đó:</strong> {activePartner.rejectionReason}
              </div>
            )}

            {activePartner.approvalNotice && (
              <div className="p-3 rounded-xl bg-sky-950/30 border border-sky-500/30 text-sky-300">
                <strong>Lời nhắn phê duyệt:</strong> {activePartner.approvalNotice}
              </div>
            )}

            <div className="flex justify-end pt-2">
              <Button variant="outline" size="sm" onClick={() => setModalMode(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

export default function AdminPartnersPage() {
  return (
    <Suspense
      fallback={
        <div className="space-y-6">
          <Skeleton className="h-10 w-64 rounded-xl" />
          <Skeleton className="h-96 w-full rounded-2xl" />
        </div>
      }
    >
      <PartnersCrmContent />
    </Suspense>
  );
}
