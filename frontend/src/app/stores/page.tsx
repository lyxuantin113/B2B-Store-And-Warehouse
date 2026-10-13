"use client";

import React, { useState, useEffect, useCallback } from "react";
import { NearbyBranch, StandardizedAddress } from "@/types/branch";
import { api } from "@/lib/api-client";
import { BranchCard } from "@/components/store-locator/branch-card";
import { RadiusFilter } from "@/components/store-locator/radius-filter";
import { StoreMap } from "@/components/store-locator/store-map";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/context/toast-context";
import { Search, LocateFixed, Store, SlidersHorizontal, AlertCircle } from "lucide-react";

export default function StoreLocatorPage() {
  const { error, info } = useToast();

  const [addressInput, setAddressInput] = useState("");
  const [selectedRadiusKm, setSelectedRadiusKm] = useState(15);
  const [selectedCategory, setSelectedCategory] = useState<string>("");
  const [keyword, setKeyword] = useState("");

  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [branches, setBranches] = useState<NearbyBranch[]>([]);
  const [selectedBranch, setSelectedBranch] = useState<NearbyBranch | null>(null);
  const [loading, setLoading] = useState(false);

  // Search branches using coordinates or address
  const fetchBranches = useCallback(async (
    lat?: number,
    lng?: number,
    addressQuery?: string,
    radius: number = selectedRadiusKm,
    cat: string = selectedCategory
  ) => {
    setLoading(true);
    try {
      let url = "/api/v1/public/store-locator/nearest?";
      const params = new URLSearchParams();
      params.set("radiusKm", radius.toString());
      if (cat) params.set("category", cat);
      if (keyword) params.set("keyword", keyword);

      if (lat != null && lng != null) {
        params.set("lat", lat.toString());
        params.set("lng", lng.toString());
      } else if (addressQuery) {
        params.set("address", addressQuery);
      } else {
        // Default to Ho Chi Minh City Center
        params.set("lat", "10.7769");
        params.set("lng", "106.7009");
      }

      const res = await api.get<NearbyBranch[]>(url + params.toString(), { requiresAuth: false });
      setBranches(res);
      if (res.length > 0) {
        setSelectedBranch(res[0]);
      } else {
        setSelectedBranch(null);
      }
    } catch (err: any) {
      console.warn("Could not fetch nearby branches", err);
      error("Không thể tải danh sách chi nhánh. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }, [selectedRadiusKm, selectedCategory, keyword, error]);

  // Request browser GPS position
  const handleLocateMe = () => {
    if (!navigator.geolocation) {
      error("Trình duyệt của bạn không hỗ trợ định vị GPS.");
      return;
    }

    info("Đang lấy vị trí GPS của bạn...");
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const coords = {
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
        };
        setUserLocation(coords);
        fetchBranches(coords.lat, coords.lng, undefined, selectedRadiusKm);
        info("Đã cập nhật vị trí GPS thành công!");
      },
      (err) => {
        console.warn("Geolocation error", err);
        error("Không thể lấy vị trí GPS. Vui lòng nhập địa chỉ vào ô tìm kiếm.");
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  };

  // Initial load: Default to center coordinates
  useEffect(() => {
    const defaultCoords = { lat: 10.7769, lng: 106.7009 };
    setUserLocation(defaultCoords);
    fetchBranches(defaultCoords.lat, defaultCoords.lng, undefined, 15);
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!addressInput.trim()) {
      handleLocateMe();
      return;
    }
    fetchBranches(undefined, undefined, addressInput.trim(), selectedRadiusKm);
  };

  const handleRadiusChange = (radius: number) => {
    setSelectedRadiusKm(radius);
    if (userLocation) {
      fetchBranches(userLocation.lat, userLocation.lng, undefined, radius);
    } else if (addressInput) {
      fetchBranches(undefined, undefined, addressInput.trim(), radius);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Top Banner & Search Toolbar */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6 backdrop-blur-sm space-y-5">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-black text-white flex items-center gap-2.5">
              <Store className="w-6 h-6 text-brand-400" />
              <span>Tìm Đại Lý & Kho Hàng Gần Nhất</span>
            </h1>
            <p className="text-xs text-slate-400 mt-1">
              Hệ thống định vị không gian PostGIS kết hợp Google Maps tìm kiếm chi nhánh trong bán kính thực tế.
            </p>
          </div>

          <Button
            onClick={handleLocateMe}
            variant="outline"
            size="md"
            className="gap-2 shrink-0 border-brand-500/40 text-brand-300 hover:bg-brand-500/10"
          >
            <LocateFixed className="w-4 h-4 text-brand-400" />
            <span>Vị trí GPS của tôi</span>
          </Button>
        </div>

        {/* Search Bar Form */}
        <form onSubmit={handleSearchSubmit} className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Input
              value={addressInput}
              onChange={(e) => setAddressInput(e.target.value)}
              placeholder="Nhập địa chỉ (vd: 123 Nguyễn Văn Linh, Quận 7, TP.HCM)..."
              leftIcon={<Search className="w-4 h-4 text-slate-400" />}
              className="bg-slate-950 h-11 text-sm rounded-xl"
            />
          </div>

          <Button type="submit" variant="primary" size="md" loading={loading} className="h-11 px-6 rounded-xl">
            <span>Tìm kiếm</span>
          </Button>
        </form>

        {/* Radius Filter Pills */}
        <div className="pt-2 border-t border-slate-800/80 flex flex-col sm:flex-row sm:items-center gap-3">
          <span className="text-xs font-semibold uppercase text-slate-400 tracking-wider shrink-0">
            Bán kính tìm kiếm:
          </span>
          <RadiusFilter
            selectedRadiusKm={selectedRadiusKm}
            onSelectRadius={handleRadiusChange}
          />
        </div>
      </div>

      {/* Main 2-Column Store Locator Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Branch Directory List (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">
              Kết quả ({branches.length} điểm)
            </span>
            <span className="text-[11px] text-slate-500">
              Ưu tiên điểm ghim ⭐ & khoảng cách gần nhất
            </span>
          </div>

          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="p-4 rounded-2xl border border-slate-800 bg-slate-900/40 space-y-3">
                  <Skeleton className="h-5 w-3/4" />
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-1/2" />
                </div>
              ))}
            </div>
          ) : branches.length === 0 ? (
            <div className="p-8 rounded-2xl border border-slate-800 bg-slate-900/40 text-center space-y-3">
              <AlertCircle className="w-10 h-10 text-amber-400 mx-auto" />
              <h3 className="text-sm font-bold text-white">Không tìm thấy chi nhánh trong bán kính này</h3>
              <p className="text-xs text-slate-400 max-w-xs mx-auto">
                Vui lòng mở rộng bán kính tìm kiếm lên 50km, 100km hoặc chọn "Toàn quốc" để xem tất cả kho hàng.
              </p>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleRadiusChange(2000)}
                className="mt-2 text-xs"
              >
                Xem toàn quốc
              </Button>
            </div>
          ) : (
            <div className="space-y-3 max-h-[680px] overflow-y-auto pr-1">
              {branches.map((b) => (
                <BranchCard
                  key={b.id}
                  branch={b}
                  isSelected={selectedBranch?.id === b.id}
                  onSelect={(selected) => setSelectedBranch(selected)}
                />
              ))}
            </div>
          )}
        </div>

        {/* Right Column: Interactive Map Visualizer (7 cols) */}
        <div className="lg:col-span-7 h-full sticky top-24">
          <StoreMap
            userLocation={userLocation}
            radiusKm={selectedRadiusKm}
            branches={branches}
            selectedBranch={selectedBranch}
            onSelectBranch={(b) => setSelectedBranch(b)}
          />
        </div>
      </div>
    </div>
  );
}
