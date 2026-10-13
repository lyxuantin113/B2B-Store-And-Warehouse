"use client";

import React, { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api-client";
import { useToast } from "@/context/toast-context";
import { Product, Category, ProductPriceTier } from "@/types/catalog";
import { formatVnd } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Skeleton } from "@/components/ui/skeleton";
import {
  ShoppingBag,
  Plus,
  Edit3,
  SlidersHorizontal,
  Search,
  Gift,
  Layers,
  Trash2,
  RefreshCw,
  Package,
} from "lucide-react";

export default function AdminCatalogPage() {
  const { success, error } = useToast();

  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCatId, setSelectedCatId] = useState<string>("");

  // Dynamic Price Tiers Modal
  const [tierModalProduct, setTierModalProduct] = useState<Product | null>(null);
  const [tiersForm, setTiersForm] = useState<ProductPriceTier[]>([]);
  const [savingTiers, setSavingTiers] = useState(false);

  // Sample Policy Modal
  const [sampleModalProduct, setSampleModalProduct] = useState<Product | null>(null);
  const [sampleEnabled, setSampleEnabled] = useState(true);
  const [sampleMaxQty, setSampleMaxQty] = useState(2);
  const [savingSample, setSavingSample] = useState(false);

  const fetchCatalog = useCallback(async () => {
    setLoading(true);
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
  }, []);

  useEffect(() => {
    fetchCatalog();
  }, [fetchCatalog]);

  // Open Tier Config
  const handleOpenTiersModal = (product: Product) => {
    setTierModalProduct(product);
    setTiersForm(
      product.priceTiers && product.priceTiers.length > 0
        ? [...product.priceTiers]
        : [
            { minQty: 1, maxQty: 3, unitPrice: product.basePrice, tierLabel: "1–3 Thùng" },
            { minQty: 4, maxQty: 6, unitPrice: Math.round(product.basePrice * 0.95), tierLabel: "4–6 Thùng" },
            { minQty: 7, maxQty: null, unitPrice: Math.round(product.basePrice * 0.9), tierLabel: ">= 7 Thùng" },
          ]
    );
  };

  const handleSaveTiers = async () => {
    if (!tierModalProduct) return;
    setSavingTiers(true);
    try {
      await api.put(`/api/v1/admin/catalog/products/${tierModalProduct.id}/price-tiers`, tiersForm);
      success(`Đã cập nhật các bậc giá sỉ động cho ${tierModalProduct.name}!`);
      setTierModalProduct(null);
      fetchCatalog();
    } catch (err: any) {
      console.error("Save tiers failed", err);
      error(err.message || "Cập nhật bậc giá sỉ thất bại. Vui lòng kiểm tra dải số lượng liên tục.");
    } finally {
      setSavingTiers(false);
    }
  };

  // Open Sample Policy Config
  const handleOpenSampleModal = (product: Product) => {
    setSampleModalProduct(product);
    setSampleEnabled(product.sampleAllowed ?? product.sampleEnabled ?? true);
    setSampleMaxQty(product.maxSampleQty ?? product.sampleMaxQty ?? 2);
  };

  const handleSaveSample = async () => {
    if (!sampleModalProduct) return;
    setSavingSample(true);
    try {
      // Update product sample quota
      await api.put(`/api/v1/admin/catalog/products/${sampleModalProduct.id}`, {
        sku: sampleModalProduct.sku,
        name: sampleModalProduct.name,
        unitLabel: sampleModalProduct.unitLabel,
        retailPrice: sampleModalProduct.retailPrice || sampleModalProduct.basePrice,
        baseWholesalePrice: sampleModalProduct.baseWholesalePrice || sampleModalProduct.basePrice,
        sampleEnabled,
        sampleMaxQty,
        unitsPerBox: sampleModalProduct.unitsPerBox || 1,
        weightGrams: 500,
        priceTiers: sampleModalProduct.priceTiers || [],
      });
      success(`Đã cập nhật chính sách gửi mẫu thử cho ${sampleModalProduct.name}!`);
      setSampleModalProduct(null);
      fetchCatalog();
    } catch (err: any) {
      console.error("Save sample failed", err);
      error(err.message || "Không thể cập nhật chính sách mẫu thử");
    } finally {
      setSavingSample(false);
    }
  };

  const filteredProducts = products.filter((p) => {
    const matchCat = !selectedCatId || String(p.categoryId) === selectedCatId;
    const matchSearch =
      !searchTerm.trim() ||
      p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.sku.toLowerCase().includes(searchTerm.toLowerCase());
    return matchCat && matchSearch;
  });

  return (
    <div className="space-y-6">
      {/* Title */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-2.5">
            <ShoppingBag className="w-6 h-6 text-brand-400" />
            Danh Mục & Bậc Giá Sỉ Động (Catalog CMS)
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Cấu hình bảng giá sỉ linh hoạt theo số lượng, hạn mức mẫu thử miễn phí và thông số bao bì B2B
          </p>
        </div>

        <Button variant="outline" size="sm" onClick={fetchCatalog} className="text-xs">
          <RefreshCw className="w-3.5 h-3.5 mr-1" />
          Làm mới
        </Button>
      </div>

      {/* Top Filter Bar */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto no-scrollbar">
          <button
            onClick={() => setSelectedCatId("")}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors ${
              !selectedCatId
                ? "bg-brand-600 text-white shadow-sm"
                : "bg-slate-950 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            Tất cả danh mục ({products.length})
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              onClick={() => setSelectedCatId(String(c.id))}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors ${
                selectedCatId === String(c.id)
                  ? "bg-brand-600 text-white shadow-sm"
                  : "bg-slate-950 text-slate-400 hover:text-white border border-slate-800"
              }`}
            >
              {c.name}
            </button>
          ))}
        </div>

        <div className="w-full md:w-64">
          <Input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo SKU, tên sản phẩm..."
            leftIcon={<Search className="w-4 h-4 text-slate-400" />}
            className="h-9 text-xs bg-slate-950 rounded-xl"
          />
        </div>
      </div>

      {/* Products Table */}
      <Card className="border-slate-800 bg-slate-900/60 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-[11px] font-bold uppercase tracking-wider text-slate-400 select-none">
              <tr>
                <th className="p-4">SKU & Tên Sản Phẩm</th>
                <th className="p-4">Giá Bán Lẻ & Giá Sỉ Gốc</th>
                <th className="p-4">Quy Cách & Đơn Vị</th>
                <th className="p-4">Bậc Giá Sỉ Động</th>
                <th className="p-4 text-center">Chính Sách Mẫu Thử</th>
                <th className="p-4 text-right">Cấu Hình</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {loading ? (
                [1, 2, 3, 4, 5].map((i) => (
                  <tr key={i}>
                    <td colSpan={6} className="p-4">
                      <Skeleton className="h-8 w-full rounded-xl" />
                    </td>
                  </tr>
                ))
              ) : filteredProducts.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-12 text-center text-slate-500">
                    <Package className="w-8 h-8 mx-auto mb-2 opacity-50" />
                    Không tìm thấy sản phẩm nào phù hợp.
                  </td>
                </tr>
              ) : (
                filteredProducts.map((p) => {
                  const isSample = p.sampleAllowed ?? p.sampleEnabled ?? false;
                  const sampleQuota = p.maxSampleQty ?? p.sampleMaxQty ?? 2;

                  return (
                    <tr key={p.id} className="hover:bg-slate-800/30 transition-colors">
                      <td className="p-4">
                        <div className="font-bold text-white text-sm">{p.name}</div>
                        <div className="text-[11px] font-mono text-brand-300 mt-0.5">
                          SKU: {p.sku} | Nhóm: {p.categoryName || "Thực phẩm"}
                        </div>
                      </td>

                      <td className="p-4">
                        <div className="font-bold text-white text-sm">
                          {formatVnd(p.baseWholesalePrice || p.basePrice)}
                        </div>
                        <div className="text-[11px] text-slate-500">
                          Giá lẻ: {formatVnd(p.retailPrice || p.basePrice * 1.15)}
                        </div>
                      </td>

                      <td className="p-4">
                        <div className="text-white font-medium">
                          {p.packageSpec || `${p.unitsPerBox || 1} cái/thùng`}
                        </div>
                        <div className="text-[11px] text-slate-400">
                          Đơn vị tính: <strong>{p.unitLabel}</strong>
                        </div>
                      </td>

                      <td className="p-4">
                        <div className="flex flex-wrap gap-1 max-w-xs">
                          {p.priceTiers && p.priceTiers.length > 0 ? (
                            p.priceTiers.map((t, idx) => (
                              <span
                                key={idx}
                                className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-brand-300 border border-slate-700 font-mono"
                              >
                                {t.tierLabel || `>=${t.minQty}`}: {formatVnd(t.unitPrice)}
                              </span>
                            ))
                          ) : (
                            <span className="text-slate-500 text-[11px] italic">
                              Chưa tạo bậc giá sỉ
                            </span>
                          )}
                        </div>
                      </td>

                      <td className="p-4 text-center">
                        {isSample ? (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-300 border border-amber-500/30">
                            <Gift className="w-3 h-3 text-amber-400" />
                            Cho phép (Tối đa {sampleQuota})
                          </span>
                        ) : (
                          <span className="text-slate-500 text-[10px]">Không mở mẫu</span>
                        )}
                      </td>

                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleOpenTiersModal(p)}
                            className="text-xs h-7 px-2 border-brand-500/40 text-brand-300"
                          >
                            <SlidersHorizontal className="w-3.5 h-3.5 mr-1" />
                            Bậc giá sỉ
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleOpenSampleModal(p)}
                            className="text-xs h-7 px-2 text-slate-400 hover:text-white"
                          >
                            <Gift className="w-3.5 h-3.5 mr-1" />
                            Hàng mẫu
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Dynamic Price Tiers Modal */}
      {tierModalProduct && (
        <Modal
          isOpen={true}
          onClose={() => setTierModalProduct(null)}
          title={`Cấu Hình Bậc Giá Sỉ Động: ${tierModalProduct.name}`}
          maxWidth="lg"
        >
          <div className="space-y-4 py-2 text-xs">
            <p className="text-slate-300 leading-relaxed">
              Thiết lập các khoảng số lượng và đơn giá sỉ tương ứng. Khi B2B Partner đặt hàng, hệ thống tự động kiểm tra số lượng và áp giá bậc tương ứng nguyên tử.
            </p>

            <div className="space-y-2.5 max-h-72 overflow-y-auto pr-1">
              {tiersForm.map((tier, idx) => (
                <div key={idx} className="p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center gap-3">
                  <div className="w-24">
                    <label className="text-[10px] text-slate-400 block mb-1">Số lượng từ *</label>
                    <Input
                      type="number"
                      min={1}
                      value={tier.minQty}
                      onChange={(e) => {
                        const updated = [...tiersForm];
                        updated[idx].minQty = parseInt(e.target.value) || 1;
                        setTiersForm(updated);
                      }}
                      className="h-8 text-xs bg-slate-900 font-mono"
                    />
                  </div>

                  <div className="w-24">
                    <label className="text-[10px] text-slate-400 block mb-1">Đến (để trống nếu $\ge$)</label>
                    <Input
                      type="number"
                      placeholder="Max"
                      value={tier.maxQty ?? ""}
                      onChange={(e) => {
                        const updated = [...tiersForm];
                        const val = e.target.value.trim();
                        updated[idx].maxQty = val === "" ? null : parseInt(val);
                        setTiersForm(updated);
                      }}
                      className="h-8 text-xs bg-slate-900 font-mono"
                    />
                  </div>

                  <div className="flex-1">
                    <label className="text-[10px] text-slate-400 block mb-1">Đơn giá sỉ (VNĐ) *</label>
                    <Input
                      type="number"
                      step="1000"
                      value={tier.unitPrice}
                      onChange={(e) => {
                        const updated = [...tiersForm];
                        updated[idx].unitPrice = parseInt(e.target.value) || 0;
                        setTiersForm(updated);
                      }}
                      className="h-8 text-xs bg-slate-900 font-bold"
                    />
                  </div>

                  <div className="flex-1">
                    <label className="text-[10px] text-slate-400 block mb-1">Nhãn bậc hiển thị</label>
                    <Input
                      placeholder="VD: 1–3 Thùng"
                      value={tier.tierLabel || ""}
                      onChange={(e) => {
                        const updated = [...tiersForm];
                        updated[idx].tierLabel = e.target.value;
                        setTiersForm(updated);
                      }}
                      className="h-8 text-xs bg-slate-900"
                    />
                  </div>

                  {tiersForm.length > 1 && (
                    <button
                      onClick={() => setTiersForm(tiersForm.filter((_, i) => i !== idx))}
                      className="p-1.5 text-slate-500 hover:text-rose-400 self-end mb-1"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                setTiersForm([
                  ...tiersForm,
                  {
                    minQty: (tiersForm[tiersForm.length - 1]?.maxQty || 10) + 1,
                    maxQty: null,
                    unitPrice: Math.round((tierModalProduct.basePrice || 100000) * 0.85),
                    tierLabel: "Bậc tiếp theo",
                  },
                ])
              }
              className="text-xs"
            >
              <Plus className="w-3.5 h-3.5 mr-1" />
              Thêm bậc giá mới
            </Button>

            <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-800">
              <Button variant="outline" size="sm" onClick={() => setTierModalProduct(null)}>
                Hủy
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleSaveTiers}
                disabled={savingTiers}
                className="text-xs font-bold"
              >
                {savingTiers ? "Đang lưu..." : "Lưu Bảng Giá Sỉ"}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Sample Policy Modal */}
      {sampleModalProduct && (
        <Modal
          isOpen={true}
          onClose={() => setSampleModalProduct(null)}
          title={`Chính Sách Mẫu Thử: ${sampleModalProduct.name}`}
          maxWidth="sm"
        >
          <div className="space-y-4 py-2 text-xs">
            <label className="flex items-center gap-2 cursor-pointer text-slate-300">
              <input
                type="checkbox"
                checked={sampleEnabled}
                onChange={(e) => setSampleEnabled(e.target.checked)}
                className="w-4 h-4 rounded text-brand-600 bg-slate-950 border-slate-700"
              />
              <span className="font-semibold text-white">
                Cho phép đối tác B2B yêu cầu mẫu thử miễn phí
              </span>
            </label>

            {sampleEnabled && (
              <div className="space-y-1.5 pt-2 border-t border-slate-800">
                <label className="text-slate-300 font-medium">
                  Hạn mức mẫu thử tối đa cho mỗi đơn (đơn vị: {sampleModalProduct.unitLabel}) *:
                </label>
                <Input
                  type="number"
                  min={1}
                  max={10}
                  value={sampleMaxQty}
                  onChange={(e) => setSampleMaxQty(Math.max(1, parseInt(e.target.value) || 1))}
                  className="bg-slate-950 font-bold"
                />
                <p className="text-[10px] text-slate-500">
                  Giá tiền sản phẩm sẽ bằng 0 VNĐ; đối tác chỉ thanh toán phí vận chuyển thực tế.
                </p>
              </div>
            )}

            <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-800">
              <Button variant="outline" size="sm" onClick={() => setSampleModalProduct(null)}>
                Hủy
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleSaveSample}
                disabled={savingSample}
                className="text-xs font-bold"
              >
                {savingSample ? "Đang lưu..." : "Lưu Cấu Hình Mẫu"}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
