"use client";

import React from "react";
import { cn } from "@/lib/utils";

interface RadiusFilterProps {
  selectedRadiusKm: number;
  onSelectRadius: (radiusKm: number) => void;
}

const RADIUS_OPTIONS = [
  { label: "1 km", value: 1 },
  { label: "3 km", value: 3 },
  { label: "5 km", value: 5 },
  { label: "10 km", value: 10 },
  { label: "15 km", value: 15 },
  { label: "20 km", value: 20 },
  { label: "50 km", value: 50 },
  { label: "100 km", value: 100 },
  { label: "Toàn quốc", value: 2000 },
];

export function RadiusFilter({ selectedRadiusKm, onSelectRadius }: RadiusFilterProps) {
  return (
    <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar">
      {RADIUS_OPTIONS.map((opt) => {
        const isSelected = selectedRadiusKm === opt.value;
        return (
          <button
            key={opt.value}
            type="button"
            onClick={() => onSelectRadius(opt.value)}
            className={cn(
              "px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all border",
              isSelected
                ? "bg-brand-600 text-white border-brand-500 shadow-sm shadow-brand-500/20"
                : "bg-slate-900 text-slate-300 border-slate-800 hover:border-slate-700 hover:text-white"
            )}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
}
