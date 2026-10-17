"use client";

import React, { useState, useEffect, useMemo } from "react";
import { Product, CursorPage } from "@/types/catalog";
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
  Sparkles,
  Building,
  Plus,
  Minus,
  Trash2,
  Truck,
  CreditCard,
  CheckCircle2,
  ExternalLink,
  Gift,
  HelpCircle,
  Package,
  Layers,
} from "lucide-react";

interface SampleRequestTabProps {
  onOrderPlaced?: (orderCode: string) => void;
}

export function SampleRequestTab({ onOrderPlaced }: SampleRequestTabProps) {
  const { profile } = useAuth();
  const { success, error } = useToast();

  const [products, setProducts] = useState<Product[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [selectedBranchId, setSelectedBranchId] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [placingOrder, setPlacingOrder] = useState(false);

  // Cart: map of productId -> quantity
  const [cartQuantities, setCartQuantities] = useState<Record<string, number>>({});

  // Checkout form
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  const [shippingAddress, setShippingAddress] = useState("");
  const [shippingDetail, setShippingDetail] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("VIETQR");
  const [shippingProvider, setShippingProvider] = useState<ShippingProvider>("GHN");
  const [orderNote, setOrderNote] = useState("");

  // Payment result modal
  const [checkoutResult, setCheckoutResult] = useState<OrderCheckoutResponse | null>(null);
  const [showPaymentModal, setShowPaymentModal] = useState(false);

  // Load catalog & branches
  useEffect(() => {
    const fetchData = async () => {
      try {
        const [prodRes, branchList] = await Promise.all([
          api.get<Product[] | CursorPage<Product>>("/api/v1/public/catalog/products", { requiresAuth: false }),
          api.get<Branch[]>("/api/v1/public/branches", { requiresAuth: false }),
        ]);

        const items = Array.isArray(prodRes) ? prodRes : (prodRes as CursorPage<Product>)?.items || [];
        setProducts(items);
        const validBranches = Array.isArray(branchList) ? branchList : [];
        setBranches(validBranches);
        if (validBranches.length > 0) {
          setSelectedBranchId(validBranches[0].id);
        }
      } catch (err) {
        console.warn("Could not load sample catalog", err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // Autofill profile info
  useEffect(() => {
    if (profile) {
      setRecipientName(profile.contactPersonName || profile.representativeName || "");
      setRecipientPhone(profile.contactPhone || "");
      setShippingAddress(profile.shippingAddressLine || profile.addressLine || "");
      setShippingDetail(profile.shippingAddressDetail || profile.addressDetail || "");
    }
  }, [profile]);

  // Filter products eligible for samples
  const sampleEligibleProducts = useMemo(() => {
    const list = Array.isArray(products) ? products : [];
    return list.filter((p) => {
      const isEligible = p.sampleAllowed === true || p.sampleEnabled === true;
      const matchesSearch =
        !searchTerm.trim() ||
        p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.sku.toLowerCase().includes(searchTerm.toLowerCase());
      return isEligible && matchesSearch;
    });
  }, [products, searchTerm]);

  // Handle cart quantities with max quota guard
  const updateQuantity = (product: Product, delta: number) => {
    const maxQty = product.sampleMaxQty || product.maxSampleQty || 2;
    const current = cartQuantities[product.id] || 0;
    const next = Math.max(0, Math.min(maxQty, current + delta));

    setCartQuantities((prev) => {
      const updated = { ...prev };
      if (next === 0) {
        delete updated[product.id];
      } else {
        updated[product.id] = next;
      }
      return updated;
    });
  };

  const removeCartItem = (productId: string) => {
    setCartQuantities((prev) => {
      const updated = { ...prev };
      delete updated[productId];
      return updated;
    });
  };

  // Cart items array
  const cartItems = useMemo(() => {
    const list = Array.isArray(products) ? products : [];
    return Object.entries(cartQuantities)
      .map(([prodId, qty]) => {
        const product = list.find((p) => p.id === prodId);
        if (!product) return null;
        return { product, quantity: qty };
      })
      .filter(Boolean) as { product: Product; quantity: number }[];
  }, [cartQuantities, products]);

  // Shipping fee calculation estimation
  const estimatedShippingFee = useMemo(() => {
    if (shippingProvider === "AHAMOVE") return 45000;
    if (shippingProvider === "GHTK") return 35000;
    if (shippingProvider === "SELF_PICKUP") return 0;
    return 30000; // GHN default
  }, [shippingProvider]);

  const handlePlaceSampleOrder = async () => {
    if (!selectedBranchId) {
      error("Vui lòng chọn kho xuất mẫu");
      return;
    }
    if (cartItems.length === 0) {
      error("Vui lòng chọn ít nhất 1 sản phẩm mẫu");
      return;
    }
    if (!recipientName.trim() || !recipientPhone.trim() || !shippingAddress.trim()) {
      error("Vui lòng điền đầy đủ thông tin người nhận và địa chỉ giao mẫu");
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

      const res = await api.post<OrderCheckoutResponse>("/api/v1/b2b/orders/sample", payload);
      setCheckoutResult(res);
      setShowPaymentModal(true);
      setCartQuantities({});
      success(`Yêu cầu mẫu #${res.orderCode} đã tạo thành công!`);

      if (res.paymentUrl && res.requiresPaymentRedirect) {
        window.open(res.paymentUrl, "_blank");
      }
    } catch (err: any) {
      console.error("Sample order creation failed", err);
      error(err.message || "Không thể gửi yêu cầu mẫu. Vui lòng kiểm tra lại hạn mức mẫu.");
    } finally {
      setPlacingOrder(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Policy banner */}
      <div className="p-4 rounded-2xl border border-amber-500/20 bg-gradient-to-r from-amber-950/30 via-slate-900/60 to-slate-900/60 flex items-start gap-3.5">
        <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400 shrink-0 mt-0.5">
          <Gift className="w-5 h-5" />
        </div>
        <div className="space-y-1">
          <h3 className="text-sm font-bold text-white flex items-center gap-2">
            Chính Sách Cung Cấp Hàng Mẫu Dành Cho Đối Tác B2B
            <Badge variant="warning" size="sm">0 VNĐ Tiền Hàng</Badge>
          </h3>
          <p className="text-xs text-slate-300 leading-relaxed">
            Đối tác B2B được yêu cầu mẫu thử miễn phí (tối đa 1–3 đơn vị/sản phẩm) để kiểm định chất lượng bao bì và sản phẩm thực tế trước khi ký hợp đồng nhập lô lớn. 
            <strong> Tiền hàng = 0 VNĐ</strong>; đối tác chỉ thanh toán cước phí vận chuyển thực tế theo đơn vị giao hàng.
          </p>
        </div>
      </div>

      {/* Origin Branch selector & search */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3 w-full md:w-auto">
          <Building className="w-5 h-5 text-brand-400 shrink-0" />
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-slate-300 uppercase tracking-wider shrink-0">
              Kho xuất mẫu:
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
            placeholder="Tìm sản phẩm có hàng mẫu..."
            className="h-9 text-xs bg-slate-950 rounded-xl"
          />
        </div>
      </div>

      {/* Main Grid: Sample Product List (7 cols) + Sample Checkout (5 cols) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left: Sample Products */}
        <div className="lg:col-span-7 space-y-3">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">
              Danh Mục Sản Phẩm Cho Phép Thử Mẫu ({sampleEligibleProducts.length})
            </span>
            <span className="text-[11px] text-amber-400 font-medium">
              * Giới hạn theo hạn mức từng sản phẩm
            </span>
          </div>

          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-20 w-full rounded-2xl" />
              ))}
            </div>
          ) : sampleEligibleProducts.length === 0 ? (
            <Card className="p-12 text-center border-slate-800 bg-slate-900/40">
              <Package className="w-10 h-10 text-slate-600 mx-auto mb-3" />
              <p className="text-sm font-semibold text-slate-300">
                Không tìm thấy sản phẩm có chính sách thử mẫu phù hợp.
              </p>
            </Card>
          ) : (
            <div className="space-y-2.5">
              {sampleEligibleProducts.map((product) => {
                const maxQty = product.sampleMaxQty || product.maxSampleQty || 2;
                const inCart = cartQuantities[product.id] || 0;

                return (
                  <Card
                    key={product.id}
                    className="p-4 border-slate-800 bg-slate-900/70 hover:border-slate-700 transition-all flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-brand-300 border border-slate-700">
                          {product.sku}
                        </span>
                        <Badge variant="warning" size="sm">
                          Tối đa {maxQty} {product.unitLabel}
                        </Badge>
                      </div>
                      <h4 className="text-sm font-bold text-white">{product.name}</h4>
                      <p className="text-xs text-slate-400">
                        Quy cách: {product.packageSpec || `${product.unitsPerBox || 1} cái/thùng`} | Tiền mẫu:{" "}
                        <span className="line-through text-slate-500 mr-1.5">
                          {formatVnd(product.basePrice || product.retailPrice || 0)}
                        </span>
                        <span className="text-emerald-400 font-bold">0 VNĐ (Miễn phí)</span>
                      </p>
                    </div>

                    {/* Quantity controls */}
                    <div className="flex items-center gap-2 self-end sm:self-center">
                      <button
                        onClick={() => updateQuantity(product, -1)}
                        disabled={inCart === 0}
                        className="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-white flex items-center justify-center transition-colors"
                      >
                        <Minus className="w-3.5 h-3.5" />
                      </button>
                      <span className="w-8 text-center text-sm font-bold text-white">
                        {inCart}
                      </span>
                      <button
                        onClick={() => updateQuantity(product, 1)}
                        disabled={inCart >= maxQty}
                        className="w-8 h-8 rounded-lg bg-brand-600 hover:bg-brand-500 disabled:opacity-40 text-white flex items-center justify-center transition-colors"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}
        </div>

        {/* Right: Checkout Sidebar */}
        <div className="lg:col-span-5 space-y-4">
          <Card className="p-5 border-slate-800 bg-slate-900/90 space-y-4 sticky top-6">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Gift className="w-4 h-4 text-amber-400" />
                Phiếu Yêu Cầu Hàng Mẫu
              </h3>
              <Badge variant="brand" size="sm">
                {cartItems.length} mẫu đã chọn
              </Badge>
            </div>

            {/* Selected items review */}
            {cartItems.length === 0 ? (
              <p className="text-xs text-slate-500 italic py-2 text-center">
                Chưa chọn mẫu thử nào. Vui lòng bấm dấu (+) bên danh sách để thêm.
              </p>
            ) : (
              <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                {cartItems.map(({ product, quantity }) => (
                  <div
                    key={product.id}
                    className="p-2.5 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between text-xs"
                  >
                    <div>
                      <div className="font-semibold text-white">{product.name}</div>
                      <div className="text-[11px] text-slate-400">
                        SL: {quantity} {product.unitLabel} × 0 VNĐ
                      </div>
                    </div>
                    <button
                      onClick={() => removeCartItem(product.id)}
                      className="text-slate-500 hover:text-rose-400 p-1"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Shipping Info Form */}
            <div className="space-y-3 pt-2 border-t border-slate-800">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                Địa Chỉ Nhận Mẫu
              </span>

              <div className="grid grid-cols-2 gap-2">
                <Input
                  value={recipientName}
                  onChange={(e) => setRecipientName(e.target.value)}
                  placeholder="Họ tên người nhận"
                  className="h-8 text-xs bg-slate-950 rounded-lg"
                />
                <Input
                  value={recipientPhone}
                  onChange={(e) => setRecipientPhone(e.target.value)}
                  placeholder="Số điện thoại"
                  className="h-8 text-xs bg-slate-950 rounded-lg"
                />
              </div>

              <Input
                value={shippingAddress}
                onChange={(e) => setShippingAddress(e.target.value)}
                placeholder="Số nhà, đường, phường/xã, quận/huyện"
                className="h-8 text-xs bg-slate-950 rounded-lg"
              />

              <Input
                value={shippingDetail}
                onChange={(e) => setShippingDetail(e.target.value)}
                placeholder="Ghi chú giao hàng (phòng ban, tầng...)"
                className="h-8 text-xs bg-slate-950 rounded-lg"
              />
            </div>

            {/* Delivery & Payment Provider */}
            <div className="space-y-3 pt-2 border-t border-slate-800">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                Đơn Vị Vận Chuyển
              </span>
              <div className="grid grid-cols-3 gap-2 text-xs">
                {(["GHN", "AHAMOVE", "GHTK"] as ShippingProvider[]).map((prov) => (
                  <button
                    key={prov}
                    onClick={() => setShippingProvider(prov)}
                    className={cn(
                      "p-2 rounded-xl border text-center transition-colors font-medium",
                      shippingProvider === prov
                        ? "bg-brand-600/20 border-brand-500 text-white"
                        : "bg-slate-950 border-slate-800 text-slate-400 hover:text-white"
                    )}
                  >
                    {prov}
                  </button>
                ))}
              </div>

              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block pt-1">
                Hình Thức Thanh Toán Cước Vận Chuyển
              </span>
              <div className="grid grid-cols-3 gap-2 text-xs">
                {(["VIETQR", "VNPAY", "MOMO"] as PaymentMethod[]).map((pm) => (
                  <button
                    key={pm}
                    onClick={() => setPaymentMethod(pm)}
                    className={cn(
                      "p-2 rounded-xl border text-center transition-colors font-medium",
                      paymentMethod === pm
                        ? "bg-brand-600/20 border-brand-500 text-white"
                        : "bg-slate-950 border-slate-800 text-slate-400 hover:text-white"
                    )}
                  >
                    {pm}
                  </button>
                ))}
              </div>
            </div>

            {/* Fee summary */}
            <div className="space-y-2 pt-3 border-t border-slate-800 text-xs">
              <div className="flex justify-between text-slate-400">
                <span>Tiền hàng mẫu:</span>
                <span className="text-emerald-400 font-bold">0 VNĐ (Miễn phí)</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Cước vận chuyển ({shippingProvider}):</span>
                <span className="text-white font-semibold">{formatVnd(estimatedShippingFee)}</span>
              </div>
              <div className="flex justify-between text-sm font-bold pt-2 border-t border-slate-800/80 text-white">
                <span>Tổng thanh toán:</span>
                <span className="text-brand-400 text-base">{formatVnd(estimatedShippingFee)}</span>
              </div>
            </div>

            <Button
              variant="primary"
              size="lg"
              onClick={handlePlaceSampleOrder}
              disabled={placingOrder || cartItems.length === 0}
              className="w-full text-xs font-bold"
            >
              {placingOrder ? "Đang xử lý..." : "Xác Nhận Yêu Cầu Hàng Mẫu"}
            </Button>
          </Card>
        </div>
      </div>

      {/* Payment / Receipt Modal */}
      {showPaymentModal && checkoutResult && (
        <Modal
          isOpen={showPaymentModal}
          onClose={() => setShowPaymentModal(false)}
          title={`Phiếu Yêu Cầu Mẫu #${checkoutResult.orderCode}`}
          maxWidth="md"
        >
          <div className="space-y-5 text-center py-2">
            <div className="w-12 h-12 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-6 h-6" />
            </div>

            <div className="space-y-1">
              <h4 className="text-base font-bold text-white">Tiếp Nhận Đơn Mẫu Thành Công!</h4>
              <p className="text-xs text-slate-400">
                Kho đã chuẩn bị đóng gói mẫu thử. Bạn chỉ cần thanh toán cước phí vận chuyển.
              </p>
            </div>

            {/* VietQR */}
            {checkoutResult.paymentMethod === "VIETQR" && (
              <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-3">
                <div className="w-48 h-48 bg-white rounded-xl mx-auto flex items-center justify-center p-2">
                  <img
                    src={`https://api.vietqr.io/image/970422-1028746382-compact2.png?amount=${checkoutResult.totalAmount}&addInfo=UM${checkoutResult.orderCode}&accountName=UMEGO%20VIETNAM`}
                    alt="VietQR Code"
                    className="w-full h-full object-contain"
                  />
                </div>
                <p className="text-[11px] text-slate-400">
                  Quét mã QR bằng App Ngân Hàng bất kỳ. Cước vận chuyển:{" "}
                  <strong className="text-brand-300 font-mono">
                    {formatVnd(checkoutResult.totalAmount)}
                  </strong>
                </p>
              </div>
            )}

            {/* VNPay / MoMo Link */}
            {checkoutResult.paymentUrl && (
              <a
                href={checkoutResult.paymentUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-500 text-white font-bold text-xs shadow-lg transition-colors"
              >
                <span>Thanh Toán Cước Vận Chuyển ({checkoutResult.paymentMethod})</span>
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
              Xem Chi Tiết Trong Lịch Sử Đơn Hàng
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}
