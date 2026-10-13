"use client";

import React from "react";
import { cn } from "@/lib/utils";

interface TabsProps {
  tabs: {
    id: string;
    label: string;
    icon?: React.ReactNode;
    badge?: number | string;
  }[];
  activeTab: string;
  onChange: (tabId: string) => void;
  className?: string;
}

export function Tabs({ tabs, activeTab, onChange, className }: TabsProps) {
  return (
    <div className={cn("flex items-center gap-1.5 border-b border-slate-800 pb-px overflow-x-auto no-scrollbar", className)}>
      {tabs.map((tab) => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            className={cn(
              "flex items-center gap-2 px-4 py-2.5 text-xs font-semibold rounded-t-lg transition-all border-b-2 whitespace-nowrap",
              isActive
                ? "border-brand-500 text-white bg-slate-900/60"
                : "border-transparent text-slate-400 hover:text-slate-200 hover:bg-slate-900/30"
            )}
          >
            {tab.icon && <span className="text-slate-400">{tab.icon}</span>}
            <span>{tab.label}</span>
            {tab.badge != null && (
              <span
                className={cn(
                  "px-1.5 py-0.2 rounded-full text-[10px] font-bold",
                  isActive ? "bg-brand-500 text-white" : "bg-slate-800 text-slate-300"
                )}
              >
                {tab.badge}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
