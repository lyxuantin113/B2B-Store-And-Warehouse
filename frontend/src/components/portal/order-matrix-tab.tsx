"use client";

import React, { useState, useEffect, useMemo } from "react";
import { Product, ProductPriceTier } from "@/types/catalog";
import { Branch } from "@/types/branch";
import { CreateOrderRequest, OrderCheckoutResponse, PaymentMethod, ShippingProvider } from "@/types/order";
import { useAuth } from "@/context/auth-context";
import { useToast } from "@/context/toast-context";
import { api } from "@/lib/api-client";
import { formatVnd, cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Modal } from "@/components/ui/modal";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Search,
  Building,
  Plus,
  Minus,
  Trash2,
  CreditCard,
  Truck,
  QrCode,
  CheckCircle2,
  MapPin,
  ExternalLink,
  ShoppingBag,
} from "lucide-react";

interface OrderMatrixTabProps {
  onOrderPlaced?: (orderCode: string) => void;
}

export function OrderMatrixTab({ onOrderPlaced }: OrderMatrixTabProps) {
  const { profile } = useAuth();
  const { success, error } = useToast();

  const [products, setProducts] = useState<Product[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [selectedBranchId, setSelectedBranchId] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [placingOrder, setPlacingOrder] = useState(false);

  // Cart state: map of productId -> quantity
  const [cartQuantities, setCartQuantities] = useState<Record<string, number>>({});

  // Checkout form state
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  const [shippingAddress, setShippingAddress] = useState("");
  const [shippingDetail, setShippingDetail] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("VNPAY");
  const [shippingProvider, setShippingProvider] = useState<ShippingProvider>("GHN");
  const [orderNote, setOrderNote] = useState("");

  // VietQR / Payment Modal state
  const [checkoutResult, setCheckoutResult] = useState<OrderCheckoutResponse | null>(null);
  const [showPaymentModal, setShowPaymentModal] = useState(false);

  // Fetch initial catalog & branches
  useEffect(() => {
    const fetchCatalog = async () => {
      try {
        const [prodList, branchList] = await Promise.all([
          api.get<Product[]>("/api/v1/public/catalog/products", { requiresAuth: false }),
          api.get<Branch[]>("/api/v1/public/branches", { requiresAuth: false }),
        ]);

        setProducts(prodList);
        setBranches(branchList);
        if (branchList.length > 0) {
          setSelectedBranchId(branchList[0].id);
        }
      } catch (err) {
        console.warn("Could not load order matrix catalog", err);
      } finally {
        setLoading(false);
      }
    };

    fetchCatalog();
  }, []);

  // Populate recipient from partner profile
  useEffect(() => {
    if (profile) {
      setRecipientName(profile.contactPersonName || profile.representativeName || "");
      setRecipientPhone(profile.contactPhone || "");
      setShippingAddress(profile.shippingAddressLine || profile.addressLine || "");
      setShippingDetail(profile.shippingAddressDetail || profile.addressDetail || "");
    }
  }, [profile]);

  // Helper: Find applicable tier price based on quantity
  const getApplicableTierPrice = (product: Product, qty: number): number => {
    if (!product.priceTiers || product.priceTiers.length === 0 || qty <= 0) {
      return product.basePrice;
    }

    // Sort tiers ascending by minQty
    const sorted = [...product.priceTiers].sort((a, b) => a.minQty - b.minQty);
    let matchedPrice = product.basePrice;

    for (const tier of sorted) {
      if (qty >= tier.minQty) {
        if (tier.maxQty == null || qty <= tier.maxQty) {
          matchedPrice = tier.unitPrice;
          break;
        } else {
          matchedPrice = tier.unitPrice; // Use highest qualifying tier
        }
      }
    }
    return matchedPrice;
  };

  // Quantity updates
  const handleQuantityChange = (productId: string, qty: number) => {
    const safeQty = Math.max(0, qty);
    setCartQuantities((prev) => {
      if (safeQty === 0) {
        const copy = { ...prev };
        delete copy[productId];
        return copy;
      }
      return { ...prev, [productId]: safeQty };
    });
  };

  // Filtered products by search
  const filteredProducts = useMemo(() => {
    if (!searchTerm.trim()) return products;
    const s = searchTerm.toLowerCase();
    return products.filter(
      (p) => p.name.toLowerCase().includes(s) || p.sku.toLowerCase().includes(s)
    );
  }, [products, searchTerm]);

  // Calculate cart summary
  const cartItems = useMemo(() => {
    return Object.entries(cartQuantities)
      .map(([productId, qty]) => {
        const prod = products.find((p) => p.id === productId);
        if (!prod || qty <= 0) return null;
        const unitPrice = getApplicableTierPrice(prod, qty);
        return {
          product: prod,
          quantity: qty,
          unitPrice,
          lineTotal: unitPrice * qty,
        };
      })
      .filter(Boolean) as {
      product: Product;
      quantity: number;
      unitPrice: number;
      lineTotal: number;
    }[];
  }, [cartQuantities, products]);

  const subtotalAmount = useMemo(() => {
    return cartItems.reduce((acc, item) => acc + item.lineTotal, 0);
  }, [cartItems]);

  const estimatedShippingFee = useMemo(() => {
    if (cartItems.length === 0) return 0;
    if (shippingProvider === "SELF_PICKUP") return 0;
    if (shippingProvider === "AHAMOVE") return 65000; // Estimated express fee
    return subtotalAmount >= 5000000 ? 0 : 45000; // Free ship if wholesale >= 5M
  }, [cartItems.length, shippingProvider, subtotalAmount]);

  const grandTotal = subtotalAmount + estimatedShippingFee;

  // Handle Checkout Submit
  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (cartItems.length === 0) {
      error("Vui lòng chọn ít nhất 1 sản phẩm vào giỏ hàng.");
      return;
    }
    if (!selectedBranchId) {
      error("Vui lòng chọn chi nhánh xuất kho.");
      return;
    }
    if (!recipientName.trim() || !recipientPhone.trim() || !shippingAddress.trim()) {
      error("Vui lòng điền đầy đủ thông tin nhận hàng.");
      return;
    }

    setPlacingOrder(true);
    try {
      const payload: CreateOrderRequest = {
        branchId: selectedBranchId,
        paymentMethod,
        shippingProvider,
        recipientName: recipientName.trim(),
        recipientPhone: recipientPhone.trim(),
        shippingAddressLine: shippingAddress.trim(),
        shippingAddressDetail: shippingDetail.trim(),
        shippingPostalCode: profile?.postalCode || "700000",
        orderNote: orderNote.trim(),
        items: cartItems.map((ci) => ({
          productId: ci.product.id,
          quantity: ci.quantity,
        })),
      };

      const res = await api.post<OrderCheckoutResponse>("/api/v1/b2b/orders/wholesale", payload);
      setCheckoutResult(res);
      setShowPaymentModal(true);
      setCartQuantities({}); // Clear cart
      success(`Đặt đơn sỉ #${res.orderCode} thành công! Kho đã giữ chỗ hàng nguyên tử.`);

      if (res.paymentUrl && res.requiresPaymentRedirect) {
        window.open(res.paymentUrl, "_blank");
      }
    } catch (err: any) {
      console.error("Order creation failed", err);
      error(err.message || "Không thể tạo đơn hàng. Vui lòng kiểm tra lại tồn kho.");
    } finally {
      setPlacingOrder(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Warehouse & Search Bar */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3 w-full md:w-auto">
          <Building className="w-5 h-5 text-brand-400 shrink-0" />
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-slate-300 uppercase tracking-wider shrink-0">
              Kho xuất hàng:
            </span>
            <select
              value={selectedBranchId}
              onChange={(e) => setSelectedBranchId(e.target.value)}
              className="bg-slate-950 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none focus:border-brand-500"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.code}) — {b.addressLine}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="w-full md:w-72">
          <Input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo SKU hoặc tên..."
            leftIcon={<Search className="w-4 h-4 text-slate-400" />}
            className="h-9 text-xs bg-slate-950 rounded-xl"
          />
        </div>
      </div>

      {/* Main Grid: Matrix Table (7 cols) + One-Page Checkout (5 cols) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Product Matrix Table */}
        <div className="lg:col-span-7 space-y-3">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">
              Bảng Ma Trận Sản Phẩm Sỉ ({filteredProducts.length} mặt hàng)
            </span>
            <span className="text-[11px] text-brand-400 font-medium">
              * Giá sỉ tự động giảm theo bậc số lượng
            </span>
          </div>

          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3, 4].map((i) => (
                <Skeleton key={i} className="h-16 w-full rounded-2xl" />
              ))}
            </div>
          ) : (
            <div className="space-y-3">
              {filteredProducts.map((p) => {
                const qty = cartQuantities[p.id] || 0;
                const unitPrice = getApplicableTierPrice(p, qty);
                const lineTotal = unitPrice * qty;

                return (
                  <div
                    key={p.id}
                    className={cn(
                      "rounded-2xl border p-4 transition-all space-y-3",
                      qty > 0
                        ? "border-brand-500 bg-brand-950/20 shadow-sm"
                        : "border-slate-800 bg-slate-900/50 hover:border-slate-700"
                    )}
                  >
                    {/* Top Row: Name, SKU, Package Spec */}
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h4 className="text-sm font-bold text-white">{p.name}</h4>
                        <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-400">
                          <span className="font-mono text-brand-300">{p.sku}</span>
                          <span>•</span>
                          <span>Đóng gói: <strong className="text-slate-200">{p.packageSpec}</strong></span>
                        </div>
                      </div>

                      <div className="text-right">
                        <span className="text-sm font-black text-emerald-400 block">
                          {formatVnd(unitPrice)}
                        </span>
                        <span className="text-[10px] text-slate-400">/{p.unitLabel}</span>
                      </div>
                    </div>

                    {/* Middle Row: Dynamic Tier Pricing Badges (Admin configured) */}
                    <div className="flex items-center gap-1.5 flex-wrap pt-1 border-t border-slate-800/60">
                      <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">
                        Bậc giá sỉ:
                      </span>
                      {p.priceTiers && p.priceTiers.length > 0 ? (
                        p.priceTiers.map((t, idx) => {
                          const isActiveTier =
                            qty >= t.minQty && (t.maxQty == null || qty <= t.maxQty);
                          return (
                            <span
                              key={idx}
                              className={cn(
                                "px-2 py-0.5 rounded-lg text-[10px] font-bold border transition-colors",
                                isActiveTier
                                  ? "bg-brand-600 text-white border-brand-400 ring-2 ring-brand-500/30"
                                  : "bg-slate-800 text-slate-300 border-slate-700"
                              )}
                            >
                              {t.tierLabel ? `${t.tierLabel}: ` : ""}
                              {t.minQty}
                              {t.maxQty != null ? `~${t.maxQty}` : "+"} {p.unitLabel} → {formatVnd(t.unitPrice)}
                            </span>
                          );
                        })
                      ) : (
                        <span className="text-[10px] text-slate-400">
                          Giá niêm yết: {formatVnd(p.basePrice)}
                        </span>
                      )}
                    </div>

                    {/* Bottom Row: Quantity Controller & Line Total */}
                    <div className="flex items-center justify-between pt-2">
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-slate-400">Số lượng:</span>
                        <div className="flex items-center rounded-xl border border-slate-700 bg-slate-950 p-0.5">
                          <button
                            type="button"
                            onClick={() => handleQuantityChange(p.id, qty - 1)}
                            className="p-1.5 text-slate-400 hover:text-white transition-colors"
                          >
                            <Minus className="w-3.5 h-3.5" />
                          </button>
                          <input
                            type="number"
                            min="0"
                            value={qty || ""}
                            placeholder="0"
                            onChange={(e) =>
                              handleQuantityChange(p.id, parseInt(e.target.value) || 0)
                            }
                            className="w-14 text-center bg-transparent text-xs font-bold text-white focus:outline-none"
                          />
                          <button
                            type="button"
                            onClick={() => handleQuantityChange(p.id, qty + 1)}
                            className="p-1.5 text-slate-400 hover:text-white transition-colors"
                          >
                            <Plus className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>

                      {qty > 0 && (
                        <div className="text-right">
                          <span className="text-[10px] text-slate-400 block">Thành tiền:</span>
                          <span className="text-sm font-extrabold text-white">
                            {formatVnd(lineTotal)}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Column: One-Page Checkout Drawer */}
        <div className="lg:col-span-5 sticky top-24">
          <Card className="p-6 border-slate-800 bg-slate-900/90 backdrop-blur-md space-y-6 shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <ShoppingBag className="w-5 h-5 text-brand-400" />
                <h3 className="font-bold text-sm text-white">Giỏ Hàng & Thanh Toán One-Page</h3>
              </div>
              <Badge variant="brand" size="sm">
                {cartItems.length} mặt hàng
              </Badge>
            </div>

            <form onSubmit={handlePlaceOrder} className="space-y-5">
              {/* Recipient Information */}
              <div className="space-y-3">
                <span className="text-[11px] font-bold text-slate-300 uppercase tracking-wider block">
                  1. Thông tin giao nhận
                </span>
                <div className="grid grid-cols-2 gap-2">
                  <Input
                    label="Người nhận"
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                    required
                  />
                  <Input
                    label="Số điện thoại"
                    value={recipientPhone}
                    onChange={(e) => setRecipientPhone(e.target.value)}
                    required
                  />
                </div>
                <Input
                  label="Địa chỉ giao hàng"
                  value={shippingAddress}
                  onChange={(e) => setShippingAddress(e.target.value)}
                  placeholder="Số nhà, đường, phường, quận..."
                  required
                />
              </div>

              {/* Shipping Provider Selection */}
              <div className="space-y-2">
                <span className="text-[11px] font-bold text-slate-300 uppercase tracking-wider block">
                  2. Đơn vị vận chuyển
                </span>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <button
                    type="button"
                    onClick={() => setShippingProvider("GHN")}
                    className={cn(
                      "p-3 rounded-xl border text-left font-medium transition-all",
                      shippingProvider === "GHN"
                        ? "border-brand-500 bg-brand-950/40 text-white"
                        : "border-slate-800 bg-slate-950 text-slate-400 hover:border-slate-700"
                    )}
                  >
                    <span className="font-bold block text-white">Giao Hàng Nhanh</span>
                    <span className="text-[10px] text-slate-400">Tiêu chuẩn toàn quốc</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setShippingProvider("AHAMOVE")}
                    className={cn(
                      "p-3 rounded-xl border text-left font-medium transition-all",
                      shippingProvider === "AHAMOVE"
                        ? "border-brand-500 bg-brand-950/40 text-white"
                        : "border-slate-800 bg-slate-950 text-slate-400 hover:border-slate-700"
                    )}
                  >
                    <span className="font-bold block text-white">AhaMove Hỏa Tốc</span>
                    <span className="text-[10px] text-slate-400">Trong ngày 3~12h (GPS)</span>
                  </button>
                </div>
              </div>

              {/* Payment Gateway Selection */}
              <div className="space-y-2">
                <span className="text-[11px] font-bold text-slate-300 uppercase tracking-wider block">
                  3. Phương thức thanh toán
                </span>
                <div className="grid grid-cols-3 gap-2 text-xs">
                  <button
                    type="button"
                    onClick={() => setPaymentMethod("VNPAY")}
                    className={cn(
                      "p-2.5 rounded-xl border text-center font-medium transition-all",
                      paymentMethod === "VNPAY"
                        ? "border-brand-500 bg-brand-950/40 text-white font-bold"
                        : "border-slate-800 bg-slate-950 text-slate-400"
                    )}
                  >
                    VNPay (ATM/QR)
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod("VIETQR")}
                    className={cn(
                      "p-2.5 rounded-xl border text-center font-medium transition-all",
                      paymentMethod === "VIETQR"
                        ? "border-brand-500 bg-brand-950/40 text-white font-bold"
                        : "border-slate-800 bg-slate-950 text-slate-400"
                    )}
                  >
                    VietQR Napas
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod("MOMO")}
                    className={cn(
                      "p-2.5 rounded-xl border text-center font-medium transition-all",
                      paymentMethod === "MOMO"
                        ? "border-brand-500 bg-brand-950/40 text-white font-bold"
                        : "border-slate-800 bg-slate-950 text-slate-400"
                    )}
                  >
                    Ví MoMo
                  </button>
                </div>
              </div>

              {/* Order Cost Breakdown */}
              <div className="pt-3 border-t border-slate-800 space-y-2 text-xs">
                <div className="flex justify-between text-slate-400">
                  <span>Tạm tính tiền hàng:</span>
                  <span className="text-white font-semibold">{formatVnd(subtotalAmount)}</span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Phí vận chuyển ({shippingProvider}):</span>
                  <span className="text-white font-semibold">
                    {estimatedShippingFee === 0 ? "Miễn phí" : formatVnd(estimatedShippingFee)}
                  </span>
                </div>
                <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-slate-800">
                  <span>Tổng thanh toán:</span>
                  <span className="text-emerald-400 font-black text-base">
                    {formatVnd(grandTotal)}
                  </span>
                </div>
              </div>

              {/* Place Order CTA */}
              <Button
                type="submit"
                variant="primary"
                size="lg"
                loading={placingOrder}
                disabled={cartItems.length === 0}
                className="w-full font-bold shadow-lg shadow-brand-600/30"
              >
                <span>Xác nhận Đặt Hàng Sỉ</span>
              </Button>
            </form>
          </Card>
        </div>
      </div>

      {/* Payment / VietQR Confirmation Modal */}
      {showPaymentModal && checkoutResult && (
        <Modal
          isOpen={showPaymentModal}
          onClose={() => setShowPaymentModal(false)}
          title={`Đặt Đơn Hàng Sỉ #${checkoutResult.orderCode}`}
          maxWidth="md"
        >
          <div className="space-y-5 text-center py-2">
            <div className="w-12 h-12 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-6 h-6" />
            </div>

            <div className="space-y-1">
              <h4 className="text-base font-bold text-white">Đơn Hàng Đã Được Tiếp Nhận!</h4>
              <p className="text-xs text-slate-400">
                Tồn kho đã được giữ chỗ nguyên tử tại kho xuất hàng.
              </p>
            </div>

            {/* If VietQR */}
            {checkoutResult.paymentMethod === "VIETQR" && (
              <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-3">
                <div className="w-48 h-48 bg-white rounded-xl mx-auto flex items-center justify-center p-2">
                  {/* VietQR Dynamic Napas247 QR */}
                  <img
                    src={`https://api.vietqr.io/image/970422-1028746382-compact2.png?amount=${checkoutResult.totalAmount}&addInfo=UM${checkoutResult.orderCode}&accountName=UMEGO%20VIETNAM`}
                    alt="VietQR Code"
                    className="w-full h-full object-contain"
                  />
                </div>
                <p className="text-[11px] text-slate-400">
                  Quét mã QR bằng App Ngân Hàng bất kỳ. Nội dung: <strong className="text-brand-300 font-mono">UM{checkoutResult.orderCode}</strong>
                </p>
              </div>
            )}

            {/* If VNPay or MoMo URL */}
            {checkoutResult.paymentUrl && (
              <a
                href={checkoutResult.paymentUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-500 text-white font-bold text-xs shadow-lg transition-colors"
              >
                <span>Mở Cổng Thanh Toán ({checkoutResult.paymentMethod})</span>
                <ExternalLink className="w-4 h-4" />
              </a>
            )}

            <Button
              variant="outline"
              size="md"
              onClick={() => {
                setShowPaymentModal(false);
                onOrderPlaced?.(checkoutResult.orderCode);
              }}
              className="w-full text-xs"
            >
              Xem Đơn Hàng Trong Lịch Sử
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}
