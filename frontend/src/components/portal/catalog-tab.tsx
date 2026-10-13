"use client";

import React, { useState, useEffect, useMemo } from "react";
import { Product, Category } from "@/types/catalog";
import { api } from "@/lib/api-client";
import { formatVnd } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Search, Tag, Package, Layers, Gift } from "lucide-react";

export function CatalogTab() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCatId, setSelectedCatId] = useState<string>("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchCatalog = async () => {
      try {
        const [prodList, catList] = await Promise.all([
          api.get<Product[]>("/api/v1/public/catalog/products", { requiresAuth: false }),
          api.get<Category[]>("/api/v1/public/catalog/categories", { requiresAuth: false }),
        ]);
        setProducts(prodList);
        setCategories(catList);
      } catch (err) {
        console.warn("Could not load catalog", err);
      } finally {
        setLoading(false);
      }
    };
    fetchCatalog();
  }, []);

  const filtered = useMemo(() => {
    return products.filter((p) => {
      const matchCat = !selectedCatId || p.categoryId === selectedCatId;
      const matchSearch =
        !search.trim() ||
        p.name.toLowerCase().includes(search.toLowerCase()) ||
        p.sku.toLowerCase().includes(search.toLowerCase());
      return matchCat && matchSearch;
    });
  }, [products, selectedCatId, search]);

  return (
    <div className="space-y-6">
      {/* Category Pills & Search */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Category Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto no-scrollbar">
          <button
            onClick={() => setSelectedCatId("")}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors ${
              !selectedCatId
                ? "bg-brand-600 text-white shadow-sm"
                : "bg-slate-800 text-slate-300 hover:text-white"
            }`}
          >
            Tất cả danh mục
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              onClick={() => setSelectedCatId(c.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors ${
                selectedCatId === c.id
                  ? "bg-brand-600 text-white shadow-sm"
                  : "bg-slate-800 text-slate-300 hover:text-white"
              }`}
            >
              {c.name}
            </button>
          ))}
        </div>

        {/* Search Input */}
        <div className="w-full md:w-72">
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm theo SKU hoặc tên..."
            leftIcon={<Search className="w-4 h-4 text-slate-400" />}
            className="h-9 text-xs bg-slate-950 rounded-xl"
          />
        </div>
      </div>

      {/* Catalog Grid */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <Skeleton key={i} className="h-64 rounded-2xl" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map((p) => (
            <Card key={p.id} className="p-5 border-slate-800 bg-slate-900/60 space-y-4 flex flex-col justify-between">
              <div className="space-y-3">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span className="text-[10px] font-mono text-brand-300 uppercase font-bold">
                      {p.sku}
                    </span>
                    <h3 className="font-bold text-sm text-white mt-0.5">{p.name}</h3>
                  </div>
                  {p.sampleAllowed && (
                    <Badge variant="success" size="sm" className="gap-1 shrink-0">
                      <Gift className="w-3 h-3 text-emerald-400" /> Mẫu 0đ
                    </Badge>
                  )}
                </div>

                <div className="flex items-center gap-2 text-xs text-slate-400">
                  <Package className="w-3.5 h-3.5 text-slate-500" />
                  <span>Quy cách: <strong className="text-slate-200">{p.packageSpec}</strong></span>
                </div>

                {/* Tier Pricing Table */}
                <div className="space-y-1.5 pt-2 border-t border-slate-800/80">
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                    Bảng giá sỉ bậc thang:
                  </span>
                  <div className="space-y-1">
                    {p.priceTiers && p.priceTiers.length > 0 ? (
                      p.priceTiers.map((tier, idx) => (
                        <div
                          key={idx}
                          className="flex items-center justify-between px-2.5 py-1 rounded-lg bg-slate-950/80 border border-slate-800 text-xs"
                        >
                          <span className="text-slate-300">
                            {tier.tierLabel ? `${tier.tierLabel} (` : ""}
                            Từ {tier.minQty} {tier.maxQty ? `~ ${tier.maxQty}` : "+"} {p.unitLabel}
                            {tier.tierLabel ? ")" : ""}
                          </span>
                          <span className="font-bold text-emerald-400">
                            {formatVnd(tier.unitPrice)}
                          </span>
                        </div>
                      ))
                    ) : (
                      <div className="text-xs text-slate-400">
                        Giá chuẩn: {formatVnd(p.basePrice)}
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-800/60 flex items-center justify-between text-xs text-slate-400">
                <span>Đơn vị: {p.unitLabel}</span>
                <span className="text-brand-400 font-semibold">Tồn kho khả dụng</span>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
