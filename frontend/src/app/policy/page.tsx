"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api-client";
import { formatDate } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FileText, Shield, Percent, Gift, Truck, RotateCcw, ArrowRight } from "lucide-react";

interface PolicyData {
  configKey: string;
  contentHtml: string;
  updatedAt?: string | null;
  updatedBy?: string | null;
}

export default function PolicyPage() {
  const [policy, setPolicy] = useState<PolicyData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPolicy = async () => {
      try {
        const data = await api.get<PolicyData>("/api/v1/public/policy", { requiresAuth: false });
        setPolicy(data);
      } catch (err) {
        console.warn("Could not load policy content", err);
      } finally {
        setLoading(false);
      }
    };
    fetchPolicy();
  }, []);

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-10">
      {/* Policy Header */}
      <div className="text-center space-y-3 pb-8 border-b border-slate-800">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-500/10 border border-brand-500/20 text-brand-300 text-xs font-semibold uppercase tracking-wider">
          <Shield className="w-3.5 h-3.5 text-brand-400" />
          <span>Văn bản Pháp lý & Quy định Hợp tác</span>
        </div>

        <h1 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
          Chính Sách Đối Tác Doanh Nghiệp (B2B)
        </h1>

        <p className="text-xs sm:text-sm text-slate-400 max-w-xl mx-auto">
          Các điều khoản áp dụng đối với khách hàng sỉ, nhà phân phối và doanh nghiệp đăng ký nhận mẫu thử tại hệ thống UMEGO Vietnam.
        </p>

        {policy?.updatedAt && (
          <p className="text-[11px] text-slate-500 pt-1">
            Cập nhật lần cuối: {formatDate(policy.updatedAt)}
          </p>
        )}
      </div>

      {/* 4 Highlight Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 space-y-2 border-slate-800 bg-slate-900/50">
          <div className="w-8 h-8 rounded-lg bg-brand-500/10 flex items-center justify-center text-brand-400">
            <Percent className="w-4 h-4" />
          </div>
          <h3 className="text-xs font-bold text-white">Bảng Giá Theo Bậc</h3>
          <p className="text-[11px] text-slate-400 leading-relaxed">
            Áp dụng giá sỉ lũy tiến theo các khoảng số lượng do Admin cấu hình cho từng sản phẩm.
          </p>
        </Card>

        <Card className="p-4 space-y-2 border-slate-800 bg-slate-900/50">
          <div className="w-8 h-8 rounded-lg bg-emerald-500/10 flex items-center justify-center text-emerald-400">
            <Gift className="w-4 h-4" />
          </div>
          <h3 className="text-xs font-bold text-white">Mẫu Thử 0 VNĐ</h3>
          <p className="text-[11px] text-slate-400 leading-relaxed">
            Đối tác ACTIVE được đăng ký mẫu miễn phí, chỉ thanh toán phí vận chuyển thực tế.
          </p>
        </Card>

        <Card className="p-4 space-y-2 border-slate-800 bg-slate-900/50">
          <div className="w-8 h-8 rounded-lg bg-sky-500/10 flex items-center justify-center text-sky-400">
            <Truck className="w-4 h-4" />
          </div>
          <h3 className="text-xs font-bold text-white">Vận Chuyển Linh Hoạt</h3>
          <p className="text-[11px] text-slate-400 leading-relaxed">
            Hỗ trợ GHN, GHTK toàn quốc hoặc giao hỏa tốc AhaMove theo tọa độ GPS kho gần nhất.
          </p>
        </Card>

        <Card className="p-4 space-y-2 border-slate-800 bg-slate-900/50">
          <div className="w-8 h-8 rounded-lg bg-amber-500/10 flex items-center justify-center text-amber-400">
            <RotateCcw className="w-4 h-4" />
          </div>
          <h3 className="text-xs font-bold text-white">Đổi Trả Trong 48h</h3>
          <p className="text-[11px] text-slate-400 leading-relaxed">
            Cam kết 1 đổi 1 đối với các lô hàng móp méo, hư hỏng do vận chuyển hoặc lỗi sản xuất.
          </p>
        </Card>
      </div>

      {/* Policy Rich Body Content */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-6 sm:p-8 backdrop-blur-sm space-y-6 text-sm text-slate-300 leading-relaxed">
        {policy?.contentHtml ? (
          <div
            className="prose prose-invert max-w-none prose-headings:text-white prose-p:text-slate-300 prose-li:text-slate-300"
            dangerouslySetInnerHTML={{ __html: policy.contentHtml }}
          />
        ) : (
          <div className="space-y-6">
            <section className="space-y-2">
              <h2 className="text-base font-bold text-white">1. Điều Kiện Trở Thành Đối Tác B2B</h2>
              <p>
                Khách hàng phải là tổ chức, hộ kinh doanh hoặc doanh nghiệp có Giấy chứng nhận Đăng ký Kinh doanh hợp pháp tại Việt Nam. Quá trình xét duyệt gồm 3 giai đoạn: Nộp hồ sơ $\rightarrow$ Duyệt sơ bộ $\rightarrow$ Kích hoạt chính thức.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-white">2. Quy Định Đặt Hàng & Thanh Toán</h2>
              <p>
                Đơn hàng sỉ phải đạt số lượng tối thiểu theo quy định của từng sản phẩm. Hệ thống hỗ trợ các hình thức thanh toán gồm: Cổng thanh toán VNPay (ATM / Visa), Ví điện tử MoMo, và Chuyển khoản ngân hàng tự động qua VietQR (PayOS / SePay).
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-white">3. Chính Sách Mẫu Thử Miễn Phí (Sample Request)</h2>
              <p>
                Mỗi đối tác B2B ACTIVE được đăng ký tối đa số lượng mẫu theo quy định của từng dòng sản phẩm. Tiền hàng sản phẩm mẫu = 0 VNĐ. Chi phí giao hàng được tính theo cước phí thực tế của đơn vị vận chuyển (GHN/GHTK/AhaMove).
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-white">4. Cam Kết Bảo Mật & Thông Tin Hợp Đồng</h2>
              <p>
                Mọi thông tin doanh nghiệp, tài liệu GPKD và dữ liệu giao dịch sỉ được mã hóa bảo mật theo chuẩn AES-256-GCM và không chia sẻ cho bên thứ ba ngoại trừ các đối tác vận chuyển và thanh toán được ủy quyền.
              </p>
            </section>
          </div>
        )}
      </div>

      {/* Bottom CTA */}
      <div className="rounded-2xl border border-brand-500/30 bg-gradient-to-r from-brand-950/60 to-slate-900 p-8 text-center space-y-4">
        <h3 className="text-lg font-bold text-white">
          Quý Doanh Nghiệp Có Thắc Mắc Về Chính Sách?
        </h3>
        <p className="text-xs text-slate-300 max-w-md mx-auto">
          Đội ngũ chuyên viên sỉ của UMEGO Vietnam sẵn sàng hỗ trợ tư vấn hợp đồng và báo giá chi tiết qua Hotline hoặc Live Chat trực tuyến.
        </p>
        <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
          <Link href="/register">
            <Button variant="primary" size="md" className="gap-2">
              <span>Đăng ký đối tác B2B</span>
              <ArrowRight className="w-4 h-4" />
            </Button>
          </Link>
          <Link href="/stores">
            <Button variant="outline" size="md">
              Xem chi nhánh kho gần nhất
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
