import React from "react";
import Link from "next/link";
import { Building, Phone, Mail, MapPin } from "lucide-react";

export function Footer() {
  return (
    <footer className="w-full border-t border-slate-800 bg-slate-950 py-12 text-slate-400 text-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 md:grid-cols-4 gap-8">
        {/* Brand statement */}
        <div className="space-y-3 md:col-span-2">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-brand-600 flex items-center justify-center font-bold text-white text-xs">
              B2B
            </div>
            <span className="font-bold text-sm text-white">UMEGO VIETNAM CO., LTD</span>
          </div>
          <p className="text-slate-400 leading-relaxed max-w-md">
            Hệ sinh thái phân phối bán lẻ và phân phối sỉ thực phẩm, mẫu thử chuyên biệt dành cho doanh nghiệp và chuỗi cửa hàng tiện lợi tại Việt Nam.
          </p>
          <div className="pt-2 text-[11px] text-slate-500 space-y-1">
            <p>Mã số doanh nghiệp (MST): 0317894562 do Sở KH&ĐT TP. Hồ Chí Minh cấp.</p>
            <p>© {new Date().getFullYear()} UMEGO Vietnam. All rights reserved.</p>
          </div>
        </div>

        {/* Quick links */}
        <div>
          <h4 className="font-semibold text-white uppercase tracking-wider text-[11px] mb-3">
            Liên kết nhanh
          </h4>
          <ul className="space-y-2">
            <li>
              <Link href="/stores" className="hover:text-white transition-colors">
                Tìm đại lý & Chi nhánh gần nhất
              </Link>
            </li>
            <li>
              <Link href="/policy" className="hover:text-white transition-colors">
                Chính sách giá sỉ & Nhận mẫu thử
              </Link>
            </li>
            <li>
              <Link href="/register" className="hover:text-white transition-colors">
                Đăng ký tài khoản Đối tác B2B
              </Link>
            </li>
            <li>
              <Link href="/login" className="hover:text-white transition-colors">
                Đăng nhập Cổng đối tác
              </Link>
            </li>
          </ul>
        </div>

        {/* Support contact */}
        <div>
          <h4 className="font-semibold text-white uppercase tracking-wider text-[11px] mb-3">
            Trung tâm hỗ trợ
          </h4>
          <ul className="space-y-2.5">
            <li className="flex items-center gap-2">
              <Phone className="w-3.5 h-3.5 text-brand-400 shrink-0" />
              <span>Hotline sỉ: 1900 6868 (8:00 - 18:00)</span>
            </li>
            <li className="flex items-center gap-2">
              <Mail className="w-3.5 h-3.5 text-brand-400 shrink-0" />
              <span>Email: b2b@umego.com.vn</span>
            </li>
            <li className="flex items-start gap-2">
              <MapPin className="w-3.5 h-3.5 text-brand-400 shrink-0 mt-0.5" />
              <span>Tòa nhà Landmark 81, P. 22, Q. Bình Thạnh, TP. Hồ Chí Minh</span>
            </li>
          </ul>
        </div>
      </div>
    </footer>
  );
}
