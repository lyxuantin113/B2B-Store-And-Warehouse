import React from "react";
import { Check, Clock, AlertCircle } from "lucide-react";
import { AccountStatus } from "@/types/auth";
import { cn } from "@/lib/utils";

interface B2bStepperProps {
  status: AccountStatus;
  rejectionReason?: string | null;
  approvalNotice?: string | null;
}

export function B2bOnboardingStepper({
  status,
  rejectionReason,
  approvalNotice,
}: B2bStepperProps) {
  // Determine current active step (1, 2, or 3)
  let currentStep = 1;
  let isRejected = status === "REJECTED";

  if (status === "ADMIN_APPROVED") {
    currentStep = 2;
  } else if (status === "ACTIVE") {
    currentStep = 3;
  }

  const steps = [
    {
      step: 1,
      title: "Giai đoạn 1: Nộp hồ sơ",
      desc: isRejected ? "Hồ sơ bị từ chối" : "Đang chờ Admin thẩm định GPKD",
    },
    {
      step: 2,
      title: "Giai đoạn 2: Duyệt sơ bộ",
      desc: "Xác nhận điều khoản & hạn mức sỉ",
    },
    {
      step: 3,
      title: "Giai đoạn 3: Kích hoạt",
      desc: "Chính thức trở thành Đối tác B2B",
    },
  ];

  return (
    <div className="w-full space-y-6">
      {/* Stepper bar */}
      <div className="relative flex items-center justify-between">
        {/* Connector line */}
        <div className="absolute top-1/2 left-6 right-6 -translate-y-1/2 h-0.5 bg-slate-800 -z-0" />

        {steps.map((item) => {
          const isCompleted = currentStep > item.step || status === "ACTIVE";
          const isCurrent = currentStep === item.step && !isCompleted;

          return (
            <div key={item.step} className="relative z-10 flex flex-col items-center text-center">
              <div
                className={cn(
                  "w-12 h-12 rounded-full flex items-center justify-center font-bold text-sm transition-all shadow-md",
                  isCompleted
                    ? "bg-emerald-600 text-white ring-4 ring-emerald-950"
                    : isCurrent && isRejected
                    ? "bg-rose-600 text-white ring-4 ring-rose-950"
                    : isCurrent
                    ? "bg-brand-600 text-white ring-4 ring-brand-950 animate-pulse"
                    : "bg-slate-800 text-slate-400 border border-slate-700"
                )}
              >
                {isCompleted ? (
                  <Check className="w-5 h-5 stroke-[3]" />
                ) : isCurrent && isRejected ? (
                  <AlertCircle className="w-5 h-5" />
                ) : isCurrent ? (
                  <Clock className="w-5 h-5" />
                ) : (
                  item.step
                )}
              </div>

              <span className="mt-3 text-xs font-semibold text-white">
                {item.title}
              </span>
              <span className="text-[11px] text-slate-400 max-w-[140px]">
                {item.desc}
              </span>
            </div>
          );
        })}
      </div>

      {/* Status Notice Banners */}
      {status === "PENDING_ADMIN" && (
        <div className="rounded-xl border border-amber-800/60 bg-amber-950/40 p-4 text-xs text-amber-200">
          <p className="font-semibold text-amber-100">⏳ Hồ sơ đang được thẩm định</p>
          <p className="mt-1 text-slate-300">
            Hồ sơ Giấy phép Đăng ký Kinh doanh của quý doanh nghiệp đang được bộ phận Quản trị viên kiểm tra trong vòng 24 giờ làm việc. Quý đối tác vui lòng giữ liên lạc qua số điện thoại đã đăng ký.
          </p>
        </div>
      )}

      {status === "REJECTED" && (
        <div className="rounded-xl border border-rose-800/80 bg-rose-950/50 p-4 text-xs text-rose-200">
          <p className="font-bold text-rose-100">❌ Hồ sơ chưa được phê duyệt</p>
          <p className="mt-1">
            <strong>Lý do từ chối:</strong> {rejectionReason || "Thông tin GPKD hoặc mã số thuế chưa khớp với dữ liệu quốc gia."}
          </p>
          <p className="mt-2 text-slate-300">
            Quý doanh nghiệp có thể cập nhật lại hồ sơ để gửi thẩm định lại mà không cần đăng ký tài khoản mới.
          </p>
        </div>
      )}

      {status === "ADMIN_APPROVED" && (
        <div className="rounded-xl border border-emerald-800/80 bg-emerald-950/50 p-4 text-xs text-emerald-200">
          <p className="font-bold text-emerald-100">🎉 Hồ sơ đã được duyệt sơ bộ!</p>
          {approvalNotice && (
            <p className="mt-1 text-slate-200">
              <strong>Ghi chú Admin:</strong> {approvalNotice}
            </p>
          )}
          <p className="mt-2 text-slate-300">
            Vui lòng nhấn nút <strong>"Xác nhận tham gia"</strong> bên dưới để hoàn tất kích hoạt tài khoản chính thức.
          </p>
        </div>
      )}

      {status === "ACTIVE" && (
        <div className="rounded-xl border border-emerald-800/60 bg-emerald-950/30 p-3 text-xs text-emerald-300 flex items-center justify-between">
          <span className="font-semibold">✅ Doanh nghiệp đã được kích hoạt B2B Partner thành công!</span>
        </div>
      )}
    </div>
  );
}
