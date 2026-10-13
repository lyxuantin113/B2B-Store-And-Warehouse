import Link from "next/link";
import { Store, ShieldCheck, Truck, Percent, Gift, ArrowRight, CheckCircle2, Building2 } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function HomePage() {
  return (
    <div className="flex flex-col min-h-screen">
      {/* Hero Section */}
      <section className="relative overflow-hidden py-20 lg:py-28 border-b border-slate-800 bg-gradient-to-b from-slate-900/50 to-slate-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-brand-500/30 bg-brand-500/10 text-brand-300 text-xs font-semibold uppercase tracking-wider mb-6">
            <span className="w-2 h-2 rounded-full bg-brand-400 animate-pulse" />
            Hệ sinh thái Bán sỉ & Mẫu thử Doanh nghiệp B2B
          </div>

          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black text-white tracking-tight leading-tight max-w-4xl mx-auto">
            Giải Pháp Cung Ứng <span className="text-transparent bg-clip-text bg-gradient-to-r from-brand-400 to-sky-300">Nguồn Hàng Sỉ Chuẩn</span> & Nhận Mẫu Thử Miễn Phí
          </h1>

          <p className="mt-6 text-base sm:text-lg text-slate-400 max-w-2xl mx-auto leading-relaxed">
            Nền tảng B2B tối ưu hóa cho chuỗi bán lẻ, đại lý phân phối và doanh nghiệp F&B tại Việt Nam với bảng giá bậc lũy tiến, mạng lưới đa kho PostGIS và cổng thanh toán VNPay, MoMo, VietQR.
          </p>

          <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
            <Link href="/register">
              <Button size="lg" variant="primary" className="shadow-lg shadow-brand-600/30 gap-2">
                <Building2 className="w-5 h-5" />
                <span>Đăng ký Tài khoản B2B</span>
              </Button>
            </Link>

            <Link href="/stores">
              <Button size="lg" variant="outline" className="gap-2">
                <Store className="w-5 h-5 text-brand-400" />
                <span>Tìm kho gần nhất theo GPS</span>
              </Button>
            </Link>
          </div>

          <div className="mt-12 flex flex-wrap items-center justify-center gap-8 text-xs text-slate-400">
            <span className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Cấp tài khoản theo dõi tiến độ duyệt
            </span>
            <span className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Bảng giá theo bậc lũy tiến tự do
            </span>
            <span className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Đăng ký nhận mẫu thử 0đ
            </span>
            <span className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Giao hỏa tốc AhaMove & chuẩn GHN
            </span>
          </div>
        </div>
      </section>

      {/* 4 Feature Cards */}
      <section className="py-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Đặc Quyền Dành Riêng Cho Đối Tác B2B
          </h2>
          <p className="mt-2 text-sm text-slate-400">
            Hạ tầng số hóa toàn diện từ kho hàng đến thanh toán & chăm sóc khách hàng trực tuyến.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="p-6 rounded-2xl border border-slate-800 bg-slate-900/60 backdrop-blur-sm space-y-3">
            <div className="w-12 h-12 rounded-xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400">
              <Percent className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-white">Bảng Giá Sỉ Động</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Các khoảng số lượng (Tier 1, 2, 3) và mức chiết khấu được thiết lập linh hoạt theo từng sản phẩm, tự động tính giá ưu đãi nhất khi đặt sỉ.
            </p>
          </div>

          <div className="p-6 rounded-2xl border border-slate-800 bg-slate-900/60 backdrop-blur-sm space-y-3">
            <div className="w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Gift className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-white">Đăng Ký Nhận Mẫu Thử</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Doanh nghiệp đối tác được yêu cầu gửi mẫu sản phẩm kiểm thử chất lượng với giá 0đ, chỉ thanh toán phí vận chuyển thực tế.
            </p>
          </div>

          <div className="p-6 rounded-2xl border border-slate-800 bg-slate-900/60 backdrop-blur-sm space-y-3">
            <div className="w-12 h-12 rounded-xl bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-sky-400">
              <Store className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-white">Store Locator PostGIS</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Định vị kho hàng hoặc cửa hàng phân phối gần nhất trong bán kính từ 1km đến 100km thông qua Google Maps Geocoding & Spatial GIST index.
            </p>
          </div>

          <div className="p-6 rounded-2xl border border-slate-800 bg-slate-900/60 backdrop-blur-sm space-y-3">
            <div className="w-12 h-12 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <Truck className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-white">Giao Vận Đa Phương Thức</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Lựa chọn giao hàng tiêu chuẩn qua GHN/GHTK hoặc nhận hàng hỏa tốc trong 3-12 giờ qua AhaMove tính cước theo tọa độ GPS kho thực tế.
            </p>
          </div>
        </div>
      </section>

      {/* CTA Box */}
      <section className="py-16 border-t border-slate-800 bg-slate-900/30">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-6">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white">
            Sẵn Sàng Gia Nhập Mạng Lưới Phân Phối Cùng UMEGO?
          </h2>
          <p className="text-sm text-slate-400 max-w-xl mx-auto">
            Chỉ mất 3 phút để gửi hồ sơ doanh nghiệp và nhận tài khoản theo dõi tiến độ xét duyệt trực tuyến.
          </p>
          <div className="flex items-center justify-center gap-4">
            <Link href="/register">
              <Button size="lg" variant="primary" className="gap-2">
                <span>Nộp hồ sơ Doanh nghiệp</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
