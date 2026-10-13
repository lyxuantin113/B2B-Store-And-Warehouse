"use client";

import React, { useState, useEffect } from "react";
import { NearbyBranch } from "@/types/branch";
import { formatDistance, cn } from "@/lib/utils";
import { Navigation, ZoomIn, ZoomOut, RotateCcw, MapPin, Star, Building2 } from "lucide-react";

interface StoreMapProps {
  userLocation: { lat: number; lng: number } | null;
  radiusKm: number;
  branches: NearbyBranch[];
  selectedBranch: NearbyBranch | null;
  onSelectBranch: (branch: NearbyBranch) => void;
}

export function StoreMap({
  userLocation,
  radiusKm,
  branches,
  selectedBranch,
  onSelectBranch,
}: StoreMapProps) {
  // Center of map: default to selected branch or user location or HCMC center
  const defaultCenter = { lat: 10.7769, lng: 106.7009 }; // TP.HCM
  const [center, setCenter] = useState(userLocation || defaultCenter);
  const [zoomLevel, setZoomLevel] = useState(1);

  useEffect(() => {
    if (selectedBranch) {
      setCenter({ lat: selectedBranch.latitude, lng: selectedBranch.longitude });
    } else if (userLocation) {
      setCenter(userLocation);
    }
  }, [selectedBranch, userLocation]);

  // Compute bounding box or relative coordinate projection for SVG representation
  // Approximate conversion: 1 degree latitude ~ 111 km, 1 degree longitude ~ 111 * cos(lat) km
  const mapCenterLat = center.lat;
  const mapCenterLng = center.lng;

  // Coordinate projection to SVG space (600x600 viewBox, center at 300, 300)
  // Scale dynamically based on radiusKm and zoomLevel
  const spanKm = Math.max(radiusKm * 2.2, 5) / zoomLevel;
  const kmPerDegLat = 111;
  const kmPerDegLng = 111 * Math.cos((mapCenterLat * Math.PI) / 180);

  const projectToSvg = (lat: number, lng: number) => {
    const deltaLatKm = (lat - mapCenterLat) * kmPerDegLat;
    const deltaLngKm = (lng - mapCenterLng) * kmPerDegLng;

    // SVG coordinates: +deltaLng is +X (right), +deltaLat is -Y (up)
    const scale = 260 / spanKm; // 260px corresponds to spanKm/2
    const x = 300 + deltaLngKm * scale;
    const y = 300 - deltaLatKm * scale;

    return { x, y };
  };

  const userSvgPos = userLocation ? projectToSvg(userLocation.lat, userLocation.lng) : { x: 300, y: 300 };
  const radiusSvgPx = (radiusKm * (260 / spanKm));

  return (
    <div className="relative w-full h-full min-h-[480px] lg:min-h-[640px] rounded-2xl border border-slate-800 bg-slate-950 overflow-hidden flex flex-col">
      {/* Map Header Toolbar */}
      <div className="absolute top-4 left-4 right-4 z-20 flex items-center justify-between pointer-events-none">
        <div className="pointer-events-auto flex items-center gap-2 bg-slate-900/90 backdrop-blur-md border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-300 shadow-lg">
          <span className="w-2 h-2 rounded-full bg-brand-400 animate-pulse" />
          <span>Bản đồ Kho & Điểm Phân Phối B2B</span>
          <span className="text-slate-500">•</span>
          <span className="font-semibold text-white">{branches.length} điểm khả dụng</span>
        </div>

        {/* Zoom Controls */}
        <div className="pointer-events-auto flex items-center gap-1 bg-slate-900/90 backdrop-blur-md border border-slate-800 rounded-xl p-1 shadow-lg">
          <button
            onClick={() => setZoomLevel((z) => Math.min(z * 1.3, 4))}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            title="Phóng to"
          >
            <ZoomIn className="w-4 h-4" />
          </button>
          <button
            onClick={() => setZoomLevel((z) => Math.max(z / 1.3, 0.4))}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            title="Thu nhỏ"
          >
            <ZoomOut className="w-4 h-4" />
          </button>
          <button
            onClick={() => {
              setZoomLevel(1);
              if (userLocation) setCenter(userLocation);
            }}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            title="Đặt lại vị trí"
          >
            <RotateCcw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Interactive Spatial Grid SVG Map */}
      <div className="relative flex-1 w-full h-full bg-slate-950">
        <svg
          viewBox="0 0 600 600"
          className="w-full h-full object-cover select-none"
          style={{ background: "radial-gradient(circle, #0b1329 0%, #030712 100%)" }}
        >
          {/* Subtle Radar & Coordinate Grid */}
          <defs>
            <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#1e293b" strokeWidth="0.5" strokeDasharray="2,2" />
            </pattern>
          </defs>
          <rect width="600" height="600" fill="url(#grid)" />

          {/* Search Radius Circle */}
          {userLocation && radiusSvgPx > 0 && (
            <g>
              <circle
                cx={userSvgPos.x}
                cy={userSvgPos.y}
                r={radiusSvgPx}
                fill="#0284c7"
                fillOpacity="0.08"
                stroke="#38bdf8"
                strokeWidth="1.5"
                strokeDasharray="4,4"
              />
              <circle
                cx={userSvgPos.x}
                cy={userSvgPos.y}
                r={Math.max(radiusSvgPx * 0.5, 5)}
                fill="none"
                stroke="#0284c7"
                strokeWidth="0.7"
                strokeDasharray="2,4"
                opacity="0.4"
              />
            </g>
          )}

          {/* User Location Marker (GPS Center) */}
          {userLocation && (
            <g transform={`translate(${userSvgPos.x}, ${userSvgPos.y})`}>
              <circle r="18" fill="#38bdf8" fillOpacity="0.2" className="animate-ping" />
              <circle r="8" fill="#0284c7" stroke="#ffffff" strokeWidth="2" />
              <circle r="3" fill="#ffffff" />
            </g>
          )}

          {/* Branch Location Markers */}
          {branches.map((b) => {
            const pos = projectToSvg(b.latitude, b.longitude);
            const isSelected = selectedBranch?.id === b.id;
            const isPinned = b.isPinned;

            return (
              <g
                key={b.id}
                transform={`translate(${pos.x}, ${pos.y})`}
                onClick={() => onSelectBranch(b)}
                className="cursor-pointer group transition-transform hover:scale-125"
              >
                {/* Highlight ring for selected branch */}
                {isSelected && (
                  <circle r="22" fill="#2563eb" fillOpacity="0.25" className="animate-pulse" />
                )}

                {/* Pin base shadow */}
                <ellipse cx="0" cy="8" rx="7" ry="3" fill="#000000" fillOpacity="0.4" />

                {/* Marker Body */}
                <path
                  d="M 0 -16 C -9 -16 -12 -9 -12 -3 C -12 5 0 10 0 10 C 0 10 12 5 12 -3 C 12 -9 9 -16 0 -16 Z"
                  fill={isPinned ? "#f59e0b" : isSelected ? "#2563eb" : "#0f766e"}
                  stroke="#ffffff"
                  strokeWidth="1.5"
                />

                {/* Inner Icon */}
                {isPinned ? (
                  <circle cx="0" cy="-6" r="3" fill="#ffffff" />
                ) : (
                  <circle cx="0" cy="-6" r="2.5" fill="#ffffff" />
                )}

                {/* Hover label */}
                <text
                  x="0"
                  y="-22"
                  textAnchor="middle"
                  fill="#ffffff"
                  fontSize="10"
                  fontWeight="bold"
                  className="opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none drop-shadow-md"
                >
                  {b.name}
                </text>
              </g>
            );
          })}
        </svg>

        {/* Selected Branch Info Card Overlay */}
        {selectedBranch && (
          <div className="absolute bottom-4 left-4 right-4 z-20 bg-slate-900/95 backdrop-blur-md border border-slate-800 rounded-2xl p-4 shadow-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 animate-in slide-in-from-bottom-2">
            <div className="space-y-1 max-w-lg">
              <div className="flex items-center gap-2">
                <span className="font-bold text-sm text-white">{selectedBranch.name}</span>
                {selectedBranch.isPinned && (
                  <span className="px-2 py-0.5 rounded-full bg-amber-950 text-amber-400 text-[10px] font-bold border border-amber-800">
                    ⭐ Ghim
                  </span>
                )}
                {selectedBranch.distanceKm != null && (
                  <span className="px-2 py-0.5 rounded-full bg-slate-800 text-brand-300 text-[10px] font-bold">
                    {selectedBranch.distanceKm} km
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-300">{selectedBranch.addressLine}</p>
            </div>

            <a
              href={`https://www.google.com/maps/dir/?api=1&destination=${selectedBranch.latitude},${selectedBranch.longitude}`}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold shadow-md transition-colors shrink-0"
            >
              <Navigation className="w-3.5 h-3.5" />
              <span>Chỉ đường trên Google Maps</span>
            </a>
          </div>
        )}
      </div>
    </div>
  );
}
