"use client";

import React, { useState, useEffect, useCallback } from "react";
import { UnifiedNotificationCenter, NotificationItem, AnnouncementItem } from "@/types/notification";
import { api } from "@/lib/api-client";
import { formatDate } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Bell,
  CheckCheck,
  Megaphone,
  Pin,
  Clock,
  PackageCheck,
  CreditCard,
  Truck,
  AlertTriangle,
  Info,
  Calendar,
  ChevronRight,
  Sparkles,
} from "lucide-react";

export function NotificationsTab() {
  const [data, setData] = useState<UnifiedNotificationCenter | null>(null);
  const [loading, setLoading] = useState(true);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);
  const [selectedAnnouncement, setSelectedAnnouncement] = useState<AnnouncementItem | null>(null);

  const fetchNotifications = useCallback(async (onlyUnread: boolean) => {
    setLoading(true);
    try {
      const res = await api.get<UnifiedNotificationCenter>(
        `/api/v1/b2b/notifications?unreadOnly=${onlyUnread}`
      );
      setData(res);
    } catch (err) {
      console.warn("Could not load notifications", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchNotifications(unreadOnly);
  }, [unreadOnly, fetchNotifications]);

  const handleMarkAsRead = async (item: NotificationItem) => {
    if (item.isRead) return;
    try {
      await api.patch(`/api/v1/b2b/notifications/${item.id}/read`, {});
      setData((prev) => {
        if (!prev) return null;
        return {
          ...prev,
          unreadCount: Math.max(0, prev.unreadCount - 1),
          personalNotifications: {
            ...prev.personalNotifications,
            items: prev.personalNotifications.items.map((n) =>
              n.id === item.id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n
            ),
          },
        };
      });
    } catch (err) {
      console.error("Mark read failed", err);
    }
  };

  const handleMarkAllRead = async () => {
    setMarkingAll(true);
    try {
      await api.patch("/api/v1/b2b/notifications/read-all", {});
      setData((prev) => {
        if (!prev) return null;
        return {
          ...prev,
          unreadCount: 0,
          personalNotifications: {
            ...prev.personalNotifications,
            items: prev.personalNotifications.items.map((n) => ({
              ...n,
              isRead: true,
              readAt: new Date().toISOString(),
            })),
          },
        };
      });
    } catch (err) {
      console.error("Mark all read failed", err);
    } finally {
      setMarkingAll(false);
    }
  };

  const getNotificationIcon = (type: string) => {
    switch (type?.toUpperCase()) {
      case "ORDER_CONFIRMED":
      case "ORDER_PLACED":
        return <PackageCheck className="w-5 h-5 text-emerald-400" />;
      case "PAYMENT_SUCCESS":
      case "PAYMENT_CONFIRMED":
        return <CreditCard className="w-5 h-5 text-blue-400" />;
      case "ORDER_SHIPPED":
      case "DELIVERING":
        return <Truck className="w-5 h-5 text-amber-400" />;
      case "SECURITY_ALERT":
      case "WARNING":
        return <AlertTriangle className="w-5 h-5 text-rose-400" />;
      default:
        return <Info className="w-5 h-5 text-sky-400" />;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header bar */}
      <div className="p-4 rounded-2xl border border-slate-800 bg-slate-900/60 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-brand-500/10 text-brand-400 border border-brand-500/20">
            <Bell className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base font-bold text-white">Trung tâm thông báo</h2>
              {data && data.unreadCount > 0 && (
                <Badge variant="danger" size="sm">
                  {data.unreadCount} chưa đọc
                </Badge>
              )}
            </div>
            <p className="text-xs text-slate-400">
              Cập nhật tức thời trạng thái đơn hàng, thanh toán và thông báo hệ thống
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-800 text-xs font-semibold">
            <button
              onClick={() => setUnreadOnly(false)}
              className={`px-3 py-1.5 rounded-lg transition-colors ${
                !unreadOnly ? "bg-brand-600 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              Tất cả
            </button>
            <button
              onClick={() => setUnreadOnly(true)}
              className={`px-3 py-1.5 rounded-lg transition-colors ${
                unreadOnly ? "bg-brand-600 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              Chưa đọc
            </button>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={handleMarkAllRead}
            disabled={markingAll || !data || data.unreadCount === 0}
            className="text-xs"
          >
            <CheckCheck className="w-4 h-4 mr-1 text-emerald-400" />
            Đọc tất cả
          </Button>
        </div>
      </div>

      {/* Broadcast Announcements Section */}
      {data?.broadcastAnnouncements && data.broadcastAnnouncements.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-amber-400">
            <Megaphone className="w-4 h-4" />
            Thông báo từ Ban Quản Trị & Nhà Phân Phối
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {data.broadcastAnnouncements.map((ann) => (
              <Card
                key={ann.id}
                onClick={() => setSelectedAnnouncement(ann)}
                className="p-4 bg-gradient-to-br from-amber-950/20 via-slate-900/60 to-slate-900/80 border-amber-500/20 hover:border-amber-500/40 cursor-pointer transition-all group"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="space-y-1.5">
                    <div className="flex items-center gap-2">
                      {ann.isPinned && (
                        <span className="flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-md bg-amber-500/20 text-amber-300 border border-amber-500/30">
                          <Pin className="w-3 h-3 rotate-45" /> Ghim
                        </span>
                      )}
                      <span className="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-slate-800 text-slate-300">
                        {ann.categoryBadge || "Chính sách"}
                      </span>
                    </div>
                    <h3 className="text-sm font-bold text-white group-hover:text-amber-300 transition-colors line-clamp-1">
                      {ann.title}
                    </h3>
                    <p className="text-xs text-slate-400 line-clamp-2">
                      {ann.contentHtml.replace(/<[^>]*>?/gm, "")}
                    </p>
                  </div>
                  <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-amber-400 group-hover:translate-x-0.5 transition-all shrink-0 mt-1" />
                </div>
                <div className="mt-3 pt-2.5 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-500">
                  <span className="flex items-center gap-1">
                    <Calendar className="w-3 h-3" />
                    {formatDate(ann.publishedAt || ann.createdAt)}
                  </span>
                  <span className="text-amber-400 group-hover:underline">Xem chi tiết</span>
                </div>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Personal Notifications List */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400">
            <Sparkles className="w-4 h-4 text-brand-400" />
            Thông báo đơn hàng & tài khoản của bạn
          </div>
          {data && (
            <span className="text-xs text-slate-500">
              Tổng số: {data.personalNotifications?.items?.length || 0}
            </span>
          )}
        </div>

        {loading ? (
          <div className="space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="p-4 rounded-xl border border-slate-800 bg-slate-900/40 flex items-center gap-4"
              >
                <Skeleton className="w-10 h-10 rounded-xl" />
                <div className="space-y-2 flex-1">
                  <Skeleton className="w-1/3 h-4" />
                  <Skeleton className="w-2/3 h-3" />
                </div>
              </div>
            ))}
          </div>
        ) : !data || data.personalNotifications?.items?.length === 0 ? (
          <Card className="p-12 text-center border-slate-800 bg-slate-900/40">
            <div className="w-14 h-14 mx-auto mb-4 rounded-2xl bg-slate-800/60 flex items-center justify-center text-slate-500">
              <Bell className="w-7 h-7" />
            </div>
            <h3 className="text-base font-bold text-white mb-1">
              {unreadOnly ? "Không có thông báo chưa đọc nào" : "Chưa có thông báo nào"}
            </h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              {unreadOnly
                ? "Bạn đã đọc hết tất cả thông báo gần đây."
                : "Khi có phát sinh đơn hàng, xác nhận kho hoặc giao vận, thông báo sẽ hiển thị tại đây."}
            </p>
          </Card>
        ) : (
          <div className="space-y-2.5">
            {data.personalNotifications.items.map((item) => (
              <div
                key={item.id}
                onClick={() => handleMarkAsRead(item)}
                className={`p-4 rounded-2xl border transition-all cursor-pointer flex items-start gap-3.5 ${
                  !item.isRead
                    ? "bg-slate-900/90 border-brand-500/30 hover:border-brand-500/50 shadow-sm shadow-brand-500/5"
                    : "bg-slate-950/60 border-slate-800/80 hover:border-slate-700 opacity-80 hover:opacity-100"
                }`}
              >
                {/* Icon */}
                <div
                  className={`p-2.5 rounded-xl border shrink-0 ${
                    !item.isRead
                      ? "bg-slate-800/80 border-slate-700"
                      : "bg-slate-900 border-slate-800 text-slate-500"
                  }`}
                >
                  {getNotificationIcon(item.notifType)}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <h4
                      className={`text-sm font-semibold truncate ${
                        !item.isRead ? "text-white" : "text-slate-300"
                      }`}
                    >
                      {item.title}
                    </h4>
                    <span className="text-[11px] text-slate-500 whitespace-nowrap flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {formatDate(item.createdAt)}
                    </span>
                  </div>

                  <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                    {item.message}
                  </p>

                  <div className="mt-2.5 flex items-center gap-2">
                    {item.referenceId && (
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-brand-300 border border-slate-700">
                        REF: {item.referenceId}
                      </span>
                    )}
                    {!item.isRead ? (
                      <span className="inline-flex items-center gap-1 text-[10px] text-brand-400 font-semibold">
                        <span className="w-1.5 h-1.5 rounded-full bg-brand-400 animate-pulse" />
                        Chưa đọc - Nhấn để xác nhận
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[10px] text-slate-500">
                        <CheckCheck className="w-3 h-3 text-slate-500" />
                        Đã đọc lúc {formatDate(item.readAt || item.createdAt)}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Announcement Detail Modal */}
      {selectedAnnouncement && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <Card className="w-full max-w-lg p-6 bg-slate-900 border-slate-700 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                {selectedAnnouncement.isPinned && (
                  <Badge variant="warning" size="sm">
                    <Pin className="w-3 h-3 mr-1" /> Ghim
                  </Badge>
                )}
                <Badge variant="neutral" size="sm">
                  {selectedAnnouncement.categoryBadge || "Chính sách"}
                </Badge>
              </div>
              <button
                onClick={() => setSelectedAnnouncement(null)}
                className="text-slate-400 hover:text-white text-sm"
              >
                ✕ Đóng
              </button>
            </div>

            <div>
              <h2 className="text-lg font-bold text-white">{selectedAnnouncement.title}</h2>
              <p className="text-xs text-slate-400 mt-1">
                Đăng lúc: {formatDate(selectedAnnouncement.publishedAt || selectedAnnouncement.createdAt)}
              </p>
            </div>

            <div
              className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-sm text-slate-300 leading-relaxed max-h-72 overflow-y-auto"
              dangerouslySetInnerHTML={{ __html: selectedAnnouncement.contentHtml }}
            />

            <div className="flex justify-end">
              <Button variant="outline" onClick={() => setSelectedAnnouncement(null)}>
                Đóng
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
