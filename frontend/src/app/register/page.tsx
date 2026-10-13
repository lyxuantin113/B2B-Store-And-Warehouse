"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/auth-context";
import { useToast } from "@/context/toast-context";
import { api } from "@/lib/api-client";
import { AuthResponse, B2bRegistrationForm } from "@/types/auth";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Building2,
  Mail,
  Lock,
  Phone,
  User,
  FileCheck,
  MapPin,
  FileText,
  ArrowRight,
  ShieldAlert,
} from "lucide-react";

export default function RegisterPage() {
  const router = useRouter();
  const { setAuthSession } = useAuth();
  const { success, error } = useToast();

  const [form, setForm] = useState<B2bRegistrationForm>({
    email: "",
    password: "",
    confirmPassword: "",
    companyName: "",
    taxCode: "",
    businessType: "Bán lẻ / Chuỗi cửa hàng",
    representativeName: "",
    contactPersonName: "",
    contactPhone: "",
    businessLicenseUrl: "https://storage.umego.vn/licenses/default-sample-license.pdf",
    postalCode: "700000",
    addressLine: "",
    addressDetail: "",
  });

  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (form.password !== form.confirmPassword) {
      error("Mật khẩu xác nhận không khớp.");
      return;
    }

    if (form.password.length < 8) {
      error("Mật khẩu phải có tối thiểu 8 ký tự.");
      return;
    }

    setLoading(true);
    try {
      const payload = {
        email: form.email.trim(),
        password: form.password,
        companyName: form.companyName.trim(),
        taxCode: form.taxCode.trim().toUpperCase(),
        businessType: form.businessType,
        representativeName: form.representativeName.trim(),
        contactPersonName: form.contactPersonName.trim(),
        contactPhone: form.contactPhone.trim(),
        businessLicenseUrl: form.businessLicenseUrl.trim(),
        postalCode: form.postalCode?.trim() || "700000",
        addressLine: form.addressLine.trim(),
        addressDetail: form.addressDetail?.trim() || "",
      };

      const res = await api.post<AuthResponse>("/api/v1/auth/register-b2b", payload, {
        requiresAuth: false,
      });

      setAuthSession(res);
      success("Đăng ký thành công! Tài khoản đang ở Giai đoạn 1: Chờ Admin xét duyệt.");
      router.push("/onboarding");
    } catch (err: any) {
      console.error("Registration error", err);
      error(err.message || "Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-12 space-y-8">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-500/10 border border-brand-500/20 text-brand-300 text-xs font-semibold uppercase tracking-wider">
          <Building2 className="w-3.5 h-3.5 text-brand-400" />
          <span>Quy trình gia nhập Đối tác B2B</span>
        </div>
        <h1 className="text-3xl font-black text-white tracking-tight">
          Đăng Ký Tài Khoản Doanh Nghiệp
        </h1>
        <p className="text-xs sm:text-sm text-slate-400 max-w-lg mx-auto">
          Cấp ngay tài khoản để theo dõi tiến độ thẩm định hồ sơ qua thanh Stepper 3 bước.
        </p>
      </div>

      {/* Main Registration Form */}
      <Card className="p-6 sm:p-10 border-slate-800 bg-slate-900/80 backdrop-blur-md">
        <form onSubmit={handleSubmit} className="space-y-8">
          {/* Section 1: Account Credentials */}
          <div className="space-y-4">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider pb-2 border-b border-slate-800 flex items-center gap-2">
              <Mail className="w-4 h-4 text-brand-400" />
              <span>1. Thông tin Tài khoản Đăng nhập</span>
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2">
                <Input
                  label="Email công ty / Đăng nhập"
                  type="email"
                  name="email"
                  value={form.email}
                  onChange={handleChange}
                  placeholder="contact@doanhnghiep.vn"
                  required
                />
              </div>
              <Input
                label="Mật khẩu (Tối thiểu 8 ký tự)"
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="••••••••"
                required
              />
              <Input
                label="Xác nhận mật khẩu"
                type="password"
                name="confirmPassword"
                value={form.confirmPassword}
                onChange={handleChange}
                placeholder="••••••••"
                required
              />
            </div>
          </div>

          {/* Section 2: Legal Company Profile */}
          <div className="space-y-4">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider pb-2 border-b border-slate-800 flex items-center gap-2">
              <Building2 className="w-4 h-4 text-brand-400" />
              <span>2. Thông tin Pháp nhân Doanh nghiệp</span>
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2">
                <Input
                  label="Tên đầy đủ của Doanh nghiệp / Hộ kinh doanh"
                  name="companyName"
                  value={form.companyName}
                  onChange={handleChange}
                  placeholder="Công ty Cổ phần Thương mại..."
                  required
                />
              </div>
              <Input
                label="Mã số thuế (MST)"
                name="taxCode"
                value={form.taxCode}
                onChange={handleChange}
                placeholder="0317894562"
                required
              />
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                  Loại hình kinh doanh
                </label>
                <select
                  name="businessType"
                  value={form.businessType}
                  onChange={handleChange}
                  className="w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-brand-500 focus:outline-none"
                >
                  <option value="Bán lẻ / Chuỗi cửa hàng">Bán lẻ / Chuỗi cửa hàng</option>
                  <option value="Đại lý phân phối F&B">Đại lý phân phối F&B</option>
                  <option value="Nhà hàng / Khách sạn / Quán Cafe">Nhà hàng / Khách sạn / Quán Cafe</option>
                  <option value="Doanh nghiệp quà tặng & Sự kiện">Doanh nghiệp quà tặng & Sự kiện</option>
                  <option value="Khác">Khác</option>
                </select>
              </div>
              <Input
                label="Người đại diện theo pháp luật"
                name="representativeName"
                value={form.representativeName}
                onChange={handleChange}
                placeholder="Nguyễn Văn A"
                required
              />
              <Input
                label="File Giấy phép ĐKKD (URL hoặc MinIO PDF/Ảnh)"
                name="businessLicenseUrl"
                value={form.businessLicenseUrl}
                onChange={handleChange}
                placeholder="https://..."
                required
              />
            </div>
          </div>

          {/* Section 3: Contact & Address */}
          <div className="space-y-4">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider pb-2 border-b border-slate-800 flex items-center gap-2">
              <MapPin className="w-4 h-4 text-brand-400" />
              <span>3. Người Liên hệ & Địa chỉ Doanh nghiệp</span>
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Người phụ trách thu mua / sỉ"
                name="contactPersonName"
                value={form.contactPersonName}
                onChange={handleChange}
                placeholder="Trần Thị B"
                required
              />
              <Input
                label="Số điện thoại liên hệ"
                name="contactPhone"
                value={form.contactPhone}
                onChange={handleChange}
                placeholder="0901234567"
                required
              />
              <div className="sm:col-span-2">
                <Input
                  label="Địa chỉ trụ sở / Kho nhận hàng chính"
                  name="addressLine"
                  value={form.addressLine}
                  onChange={handleChange}
                  placeholder="Số 123 Nguyễn Thị Minh Khai, Phường Bến Thành, Quận 1, TP.HCM"
                  required
                />
              </div>
              <Input
                label="Chi tiết địa chỉ (Tòa nhà, Lầu...)"
                name="addressDetail"
                value={form.addressDetail}
                onChange={handleChange}
                placeholder="Tầng 5, Phòng 502"
              />
              <Input
                label="Mã bưu chính (Postal Code)"
                name="postalCode"
                value={form.postalCode}
                onChange={handleChange}
                placeholder="700000"
              />
            </div>
          </div>

          {/* Submit Button */}
          <div className="pt-4 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
            <span className="text-xs text-slate-400">
              Bằng việc nộp hồ sơ, bạn đồng ý với{" "}
              <Link href="/policy" className="text-brand-400 underline">
                Chính sách B2B
              </Link>{" "}
              của UMEGO Vietnam.
            </span>

            <Button
              type="submit"
              variant="primary"
              size="lg"
              loading={loading}
              className="w-full sm:w-auto px-8 gap-2 shadow-lg shadow-brand-600/30"
            >
              <span>Nộp hồ sơ xét duyệt</span>
              <ArrowRight className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </Card>

      {/* Login link */}
      <div className="text-center text-xs text-slate-400">
        Đã có tài khoản đối tác?{" "}
        <Link href="/login" className="font-bold text-brand-400 hover:text-brand-300 underline underline-offset-4">
          Đăng nhập ngay
        </Link>
      </div>
    </div>
  );
}
