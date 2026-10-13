import React from "react";
import { cn } from "@/lib/utils";

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: "success" | "warning" | "danger" | "info" | "neutral" | "brand";
  size?: "sm" | "md";
}

export function Badge({
  className,
  variant = "neutral",
  size = "md",
  children,
  ...props
}: BadgeProps) {
  const variantStyles = {
    success: "bg-emerald-950/70 text-emerald-400 border-emerald-800",
    warning: "bg-amber-950/70 text-amber-400 border-amber-800",
    danger: "bg-rose-950/70 text-rose-400 border-rose-800",
    info: "bg-sky-950/70 text-sky-400 border-sky-800",
    brand: "bg-brand-950/70 text-brand-400 border-brand-800",
    neutral: "bg-slate-800 text-slate-300 border-slate-700",
  };

  const sizeStyles = {
    sm: "px-2 py-0.5 text-[10px] font-semibold tracking-wider",
    md: "px-2.5 py-1 text-xs font-medium",
  };

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border leading-none select-none",
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
      {...props}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current opacity-80" />
      {children}
    </span>
  );
}
