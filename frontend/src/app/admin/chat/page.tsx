"use client";

import React, { useEffect, useState, useRef, useCallback } from "react";
import { api } from "@/lib/api-client";
import { getWebSocketClient } from "@/lib/websocket-client";
import { useAuth } from "@/context/auth-context";
import { useToast } from "@/context/toast-context";
import { ChatRoom, ChatMessage, ChatRoomStatus } from "@/types/chat";
import { PageDto } from "@/types/api";
import { formatDate } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  MessageSquare,
  Search,
  Send,
  UserCheck,
  Building,
  CheckCheck,
  Clock,
  Lock,
  Unlock,
  RefreshCw,
  Sparkles,
} from "lucide-react";

export default function AdminLiveChatPage() {
  const { user } = useAuth();
  const { success, error } = useToast();

  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState("");
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState("");

  const chatBottomRef = useRef<HTMLDivElement>(null);
  const wsClientRef = useRef(getWebSocketClient());

  // Fetch Rooms
  const fetchRooms = useCallback(async () => {
    setLoadingRooms(true);
    try {
      const q = statusFilter ? `status=${statusFilter}&` : "";
      const res = await api.get<PageDto<ChatRoom>>(`/api/v1/admin/chat/rooms?${q}page=0&size=50`);
      setRooms(res.content || []);
      if (!selectedRoomId && res.content && res.content.length > 0) {
        setSelectedRoomId(res.content[0].id);
      }
    } catch (err) {
      console.warn("Could not load chat rooms", err);
    } finally {
      setLoadingRooms(false);
    }
  }, [statusFilter, selectedRoomId]);

  useEffect(() => {
    fetchRooms();
  }, [fetchRooms]);

  // Fetch Messages for active room
  const fetchMessages = useCallback(async (roomId: string) => {
    setLoadingMessages(true);
    try {
      const res = await api.get<ChatMessage[]>(`/api/v1/b2b/chat/rooms/${roomId}/messages?limit=50`);
      setMessages(res.reverse()); // Chronological order
      // Mark as read
      await api.post(`/api/v1/b2b/chat/rooms/${roomId}/read`, {});
    } catch (err) {
      console.warn("Could not load room messages", err);
    } finally {
      setLoadingMessages(false);
    }
  }, []);

  useEffect(() => {
    if (selectedRoomId) {
      fetchMessages(selectedRoomId);
    }
  }, [selectedRoomId, fetchMessages]);

  // WebSocket Subscription for Realtime
  useEffect(() => {
    const ws = wsClientRef.current;
    ws.connect();

    // Subscribe to admin notifications
    const unsubAdmin = ws.subscribeAdminNotifications((msg) => {
      // Update room list preview
      setRooms((prev) =>
        prev.map((r) =>
          r.id === msg.roomId
            ? { ...r, lastMessagePreview: msg.content, lastMessageAt: msg.createdAt, unreadCount: r.unreadCount + 1 }
            : r
        )
      );
    });

    let unsubRoom = () => {};
    if (selectedRoomId) {
      unsubRoom = ws.subscribeRoomMessages(selectedRoomId, (msg) => {
        setMessages((prev) => {
          if (prev.some((m) => m.id === msg.id)) return prev;
          return [...prev, msg];
        });
      });
    }

    return () => {
      unsubAdmin();
      unsubRoom();
    };
  }, [selectedRoomId]);

  // Auto scroll
  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Send message
  const handleSendMessage = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!inputText.trim() || !selectedRoomId || sending) return;

    const content = inputText.trim();
    setInputText("");
    setSending(true);

    try {
      const ws = wsClientRef.current;
      if (ws.isConnected()) {
        ws.sendMessage({
          roomId: selectedRoomId,
          messageType: "TEXT",
          content,
        });
      } else {
        // Fallback REST
        await api.post("/api/v1/b2b/chat/messages", {
          roomId: selectedRoomId,
          messageType: "TEXT",
          content,
        });
      }
    } catch (err) {
      console.error("Failed to send message", err);
      error("Không thể gửi tin nhắn.");
    } finally {
      setSending(false);
    }
  };

  // Assign to self
  const handleAssignSelf = async () => {
    if (!selectedRoomId || !user) return;
    try {
      await api.patch(`/api/v1/admin/chat/rooms/${selectedRoomId}/assign`, {
        assignedAdminId: user.id,
      });
      success("Đã tiếp nhận hỗ trợ đối tác này!");
      fetchRooms();
    } catch (err: any) {
      error(err.message || "Không thể phân công hỗ trợ");
    }
  };

  // Toggle room status
  const handleToggleRoomStatus = async (currentStatus: ChatRoomStatus) => {
    if (!selectedRoomId) return;
    const targetStatus = currentStatus === "OPEN" ? "CLOSED" : "OPEN";
    try {
      await api.patch(`/api/v1/admin/chat/rooms/${selectedRoomId}/status`, {
        status: targetStatus,
      });
      success(`Đã chuyển trạng thái phòng chat sang ${targetStatus}`);
      fetchRooms();
    } catch (err: any) {
      error(err.message || "Không thể cập nhật trạng thái phòng");
    }
  };

  const selectedRoom = rooms.find((r) => r.id === selectedRoomId);

  const filteredRooms = rooms.filter((r) => {
    if (!searchTerm.trim()) return true;
    const term = searchTerm.toLowerCase();
    return (
      r.partnerName.toLowerCase().includes(term) ||
      (r.partnerCompanyName && r.partnerCompanyName.toLowerCase().includes(term)) ||
      r.partnerEmail.toLowerCase().includes(term)
    );
  });

  return (
    <div className="space-y-6">
      {/* Title */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-2.5">
            <MessageSquare className="w-6 h-6 text-brand-400" />
            Bàn Làm Việc Hỗ Trợ Trực Tuyến (Live Support Agent)
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Giao tiếp thời gian thực với đối tác B2B qua WebSocket STOMP & SockJS, hỗ trợ tư vấn giá sỉ & vận đơn
          </p>
        </div>

        <Button variant="outline" size="sm" onClick={fetchRooms} className="text-xs">
          <RefreshCw className="w-3.5 h-3.5 mr-1" />
          Làm mới
        </Button>
      </div>

      {/* Main Console: Left 4 cols (Rooms) + Right 8 cols (Chat Window) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 h-[720px]">
        {/* Left: Rooms List */}
        <Card className="lg:col-span-4 border-slate-800 bg-slate-900/70 p-4 flex flex-col space-y-3 overflow-hidden">
          {/* Filter tabs */}
          <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-800 text-[11px] font-semibold">
            <button
              onClick={() => setStatusFilter("")}
              className={`flex-1 py-1 rounded-lg transition-colors ${
                !statusFilter ? "bg-brand-600 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              Tất cả
            </button>
            <button
              onClick={() => setStatusFilter("OPEN")}
              className={`flex-1 py-1 rounded-lg transition-colors ${
                statusFilter === "OPEN" ? "bg-brand-600 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              Đang Mở
            </button>
            <button
              onClick={() => setStatusFilter("CLOSED")}
              className={`flex-1 py-1 rounded-lg transition-colors ${
                statusFilter === "CLOSED" ? "bg-brand-600 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              Đã Đóng
            </button>
          </div>

          {/* Search */}
          <Input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo đối tác, công ty..."
            leftIcon={<Search className="w-3.5 h-3.5 text-slate-400" />}
            className="h-8 text-xs bg-slate-950 rounded-xl"
          />

          {/* Room Cards list */}
          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {loadingRooms ? (
              [1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-16 rounded-xl" />)
            ) : filteredRooms.length === 0 ? (
              <div className="py-12 text-center text-slate-500 text-xs">
                Không có hội thoại hỗ trợ nào.
              </div>
            ) : (
              filteredRooms.map((room) => {
                const isSelected = room.id === selectedRoomId;
                return (
                  <div
                    key={room.id}
                    onClick={() => setSelectedRoomId(room.id)}
                    className={`p-3 rounded-xl border transition-all cursor-pointer space-y-1 ${
                      isSelected
                        ? "bg-brand-600/10 border-brand-500/50 shadow-sm"
                        : "bg-slate-950/60 border-slate-800/80 hover:border-slate-700"
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <h4 className="text-xs font-bold text-white truncate max-w-[170px]">
                        {room.partnerCompanyName || room.partnerName}
                      </h4>
                      {room.unreadCount > 0 ? (
                        <Badge variant="danger" size="sm">
                          {room.unreadCount}
                        </Badge>
                      ) : (
                        <span className="text-[10px] text-slate-500">
                          {formatDate(room.lastMessageAt || room.createdAt)}
                        </span>
                      )}
                    </div>

                    <p className="text-[11px] text-slate-400 truncate">
                      {room.lastMessagePreview || "Chưa có tin nhắn"}
                    </p>

                    <div className="flex items-center justify-between pt-1 text-[10px]">
                      <span className="text-slate-500">
                        {room.assignedAdminName ? `Phụ trách: ${room.assignedAdminName}` : "Chưa nhận"}
                      </span>
                      <span
                        className={`font-semibold ${
                          room.status === "OPEN" ? "text-emerald-400" : "text-slate-500"
                        }`}
                      >
                        {room.status === "OPEN" ? "● OPEN" : "CLOSED"}
                      </span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </Card>

        {/* Right: Active Chat Window */}
        <Card className="lg:col-span-8 border-slate-800 bg-slate-900/80 flex flex-col overflow-hidden">
          {selectedRoom ? (
            <>
              {/* Header */}
              <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/50">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 rounded-xl bg-brand-500/10 text-brand-400 border border-brand-500/20">
                    <Building className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-white">
                      {selectedRoom.partnerCompanyName || selectedRoom.partnerName}
                    </h3>
                    <p className="text-xs text-slate-400">
                      Người liên hệ: <strong className="text-slate-300">{selectedRoom.partnerName}</strong> ({selectedRoom.partnerEmail})
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleAssignSelf}
                    className="text-xs h-7 border-brand-500/30 text-brand-300"
                  >
                    <UserCheck className="w-3.5 h-3.5 mr-1" />
                    Tiếp nhận
                  </Button>

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleToggleRoomStatus(selectedRoom.status)}
                    className="text-xs h-7 text-slate-400 hover:text-white"
                  >
                    {selectedRoom.status === "OPEN" ? (
                      <>
                        <Lock className="w-3.5 h-3.5 mr-1 text-rose-400" />
                        Đóng phòng
                      </>
                    ) : (
                      <>
                        <Unlock className="w-3.5 h-3.5 mr-1 text-emerald-400" />
                        Mở lại
                      </>
                    )}
                  </Button>
                </div>
              </div>

              {/* Message History */}
              <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-950/40">
                {loadingMessages ? (
                  <div className="space-y-3">
                    {[1, 2, 3].map((i) => (
                      <Skeleton key={i} className="h-12 w-2/3 rounded-xl" />
                    ))}
                  </div>
                ) : messages.length === 0 ? (
                  <div className="py-20 text-center text-slate-500 text-xs">
                    Chưa có tin nhắn trong phiên hỗ trợ này.
                  </div>
                ) : (
                  messages.map((msg) => {
                    const isStaff = msg.senderRole !== "B2B_PARTNER";
                    return (
                      <div
                        key={msg.id}
                        className={`flex flex-col ${isStaff ? "items-end" : "items-start"}`}
                      >
                        <div className="flex items-center gap-1.5 mb-1 text-[10px] text-slate-500">
                          <span className="font-semibold text-slate-400">{msg.senderName}</span>
                          <span>•</span>
                          <span>{formatDate(msg.createdAt)}</span>
                        </div>
                        <div
                          className={`max-w-[75%] p-3 rounded-2xl text-xs leading-relaxed ${
                            isStaff
                              ? "bg-brand-600 text-white rounded-tr-none shadow-md"
                              : "bg-slate-800 text-slate-200 rounded-tl-none border border-slate-700"
                          }`}
                        >
                          {msg.content}
                        </div>
                      </div>
                    );
                  })
                )}
                <div ref={chatBottomRef} />
              </div>

              {/* Chat Input */}
              <form
                onSubmit={handleSendMessage}
                className="p-3 border-t border-slate-800 bg-slate-950 flex items-center gap-2"
              >
                <Input
                  value={inputText}
                  onChange={(e) => setInputText(e.target.value)}
                  placeholder="Nhập phản hồi hỗ trợ cho đối tác B2B..."
                  disabled={selectedRoom.status === "CLOSED"}
                  className="h-10 text-xs bg-slate-900 rounded-xl"
                />
                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  disabled={!inputText.trim() || sending || selectedRoom.status === "CLOSED"}
                  className="h-10 px-4 font-bold"
                >
                  <Send className="w-4 h-4" />
                </Button>
              </form>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-slate-500 text-xs">
              Vui lòng chọn một phòng chat từ danh sách bên trái.
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
