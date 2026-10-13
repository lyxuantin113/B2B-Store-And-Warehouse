"use client";

import React from "react";
import { NearbyBranch } from "@/types/branch";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatDistance, cn } from "@/lib/utils";
import { MapPin, Phone, Clock, Navigation, Star } from "lucide-react";

interface BranchCardProps {
  branch: NearbyBranch;
  isSelected: boolean;
  onSelect: (branch: NearbyBranch) => void;
}

export function BranchCard({ branch, isSelected, onSelect }: BranchCardProps) {
  const googleMapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${branch.latitude},${branch.longitude}`;

  return (
    <div
      onClick={() => onSelect(branch)}
      className={cn(
        "cursor-pointer rounded-2xl border p-4.5 transition-all text-left space-y-3",
        isSelected
          ? "border-brand-500 bg-brand-950/20 shadow-md ring-1 ring-brand-500/50"
          : "border-slate-800 bg-slate-900/60 hover:border-slate-700 hover:bg-slate-900"
      )}
    >
      {/* Header: Title & Badges */}
      <div className="flex items-start justify-between gap-2">
        <div className="space-y-1">
          <div className="flex items-center gap-2 flex-wrap">
            <h3 className="font-bold text-sm text-white">{branch.name}</h3>
            {branch.isPinned && (
              <Badge variant="warning" size="sm" className="gap-1">
                <Star className="w-3 h-3 fill-amber-400" /> Ghim ưu tiên
              </Badge>
            )}
            {branch.categoryTag && (
              <Badge variant="brand" size="sm">
                {branch.categoryTag}
              </Badge>
            )}
          </div>
          <span className="text-[11px] font-mono text-slate-400 block">
            Mã kho: {branch.code}
          </span>
        </div>

        {branch.distanceMeters != null && (
          <div className="text-right shrink-0">
            <span className="inline-block px-2.5 py-1 rounded-lg bg-slate-800 text-brand-300 font-bold text-xs">
              {formatDistance(branch.distanceMeters)}
            </span>
          </div>
        )}
      </div>

      {/* Address */}
      <div className="flex items-start gap-2 text-xs text-slate-300 leading-relaxed">
        <MapPin className="w-4 h-4 text-brand-400 shrink-0 mt-0.5" />
        <div>
          <span>{branch.addressLine}</span>
          {branch.addressDetail && (
            <span className="text-slate-400 block mt-0.5">({branch.addressDetail})</span>
          )}
        </div>
      </div>

      {/* Details: Phone & Operating Hours */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] text-slate-400 pt-1 border-t border-slate-800/80">
        {branch.phone && (
          <div className="flex items-center gap-1.5">
            <Phone className="w-3.5 h-3.5 text-slate-500 shrink-0" />
            <a
              href={`tel:${branch.phone}`}
              onClick={(e) => e.stopPropagation()}
              className="hover:text-brand-400 transition-colors"
            >
              {branch.phone}
            </a>
          </div>
        )}
        {branch.operatingHours && (
          <div className="flex items-center gap-1.5">
            <Clock className="w-3.5 h-3.5 text-slate-500 shrink-0" />
            <span>{branch.operatingHours}</span>
          </div>
        )}
      </div>

      {/* Action Footer */}
      <div className="flex items-center justify-between pt-2">
        <span className="text-[11px] text-emerald-400 font-medium flex items-center gap-1">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
          Đang hoạt động
        </span>

        <a
          href={googleMapsUrl}
          target="_blank"
          rel="noopener noreferrer"
          onClick={(e) => e.stopPropagation()}
          className="inline-flex items-center gap-1 text-xs font-semibold text-brand-400 hover:text-brand-300 transition-colors"
        >
          <Navigation className="w-3.5 h-3.5" />
          <span>Chỉ đường Google Maps</span>
        </a>
      </div>
    </div>
  );
}
