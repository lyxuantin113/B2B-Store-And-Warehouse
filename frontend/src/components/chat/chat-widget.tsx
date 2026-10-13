"use client";

import React, { useState, useEffect, useRef } from "react";
import { MessageSquare, X, Send, Paperclip, Minimize2, Check, CheckCheck } from "lucide-react";
import { useAuth } from "@/context/auth-context";
import { ChatMessage, ChatRoom } from "@/types/chat";
import { chatSocket } from "@/lib/websocket-client";
import { api } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { cn, formatDate } from "@/lib/utils";

export function ChatWidget() {
  const { isAuthenticated, isPartner, user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [room, setRoom] = useState<ChatRoom | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState("");
  const [loading, setLoading] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Initialize room & socket connection when partner opens or logs in
  useEffect(() => {
    if (!isAuthenticated || !isPartner) return;

    let unsubscribeMessage: (() => void) | undefined;
    let unsubscribeRead: (() => void) | undefined;

    const loadRoom = async () => {
      try {
        const roomData = await api.get<ChatRoom>("/api/v1/b2b/chat/room");
        setRoom(roomData);
        setUnreadCount(roomData.unreadCount || 0);

        // Fetch recent messages
        const history = await api.get<ChatMessage[]>(
          `/api/v1/b2b/chat/rooms/${roomData.id}/messages?limit=40`
        );
        setMessages(history);

        // Connect STOMP socket
        chatSocket.connect(
          () => {
            // Subscribe to incoming messages
            unsubscribeMessage = chatSocket.subscribeRoomMessages(roomData.id, (newMsg) => {
              setMessages((prev) => {
                if (prev.some((m) => m.id === newMsg.id)) return prev;
                return [...prev, newMsg];
              });
              if (!isOpen && newMsg.senderId !== user?.id) {
                setUnreadCount((c) => c + 1);
              }
            });

            // Subscribe to read receipts
            unsubscribeRead = chatSocket.subscribeRoomReadReceipts(roomData.id, () => {
              setMessages((prev) => prev.map((m) => ({ ...m, isRead: true })));
            });
          },
          (err) => console.warn("Chat STOMP connection error", err)
        );
      } catch (err) {
        console.warn("Could not initialize chat room", err);
      }
    };

    loadRoom();

    return () => {
      unsubscribeMessage?.();
      unsubscribeRead?.();
    };
  }, [isAuthenticated, isPartner, isOpen, user?.id]);

  // Scroll to bottom when new messages arrive
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
      if (room && unreadCount > 0) {
        api.post(`/api/v1/b2b/chat/rooms/${room.id}/read`).catch(() => {});
        chatSocket.sendMarkRead(room.id);
        setUnreadCount(0);
      }
    }
  }, [messages, isOpen, room, unreadCount]);

  if (!isAuthenticated || !isPartner) {
    return null; // Only available for authenticated B2B partners
  }

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim() || !room) return;

    const textToSend = inputText.trim();
    setInputText("");

    try {
      if (chatSocket.isConnected()) {
        chatSocket.sendMessage({
          roomId: room.id,
          messageType: "TEXT",
          content: textToSend,
        });
      } else {
        // Fallback REST
        const saved = await api.post<ChatMessage>("/api/v1/b2b/chat/messages", {
          roomId: room.id,
          messageType: "TEXT",
          content: textToSend,
        });
        setMessages((prev) => [...prev, saved]);
      }
    } catch (err) {
      console.error("Failed to send message", err);
    }
  };

  return (
    <div className="fixed bottom-6 right-6 z-40">
      {/* Floating Chat Trigger Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="relative flex items-center justify-center w-14 h-14 rounded-full bg-brand-600 hover:bg-brand-500 text-white shadow-xl shadow-brand-600/30 transition-all hover:scale-105 active:scale-95"
          title="Hỗ trợ B2B trực tuyến"
        >
          <MessageSquare className="w-6 h-6" />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 flex items-center justify-center min-w-5 h-5 px-1 rounded-full bg-rose-600 text-[10px] font-black text-white ring-2 ring-slate-950 animate-bounce">
              {unreadCount}
            </span>
          )}
        </button>
      )}

      {/* Floating Chat Window */}
      {isOpen && (
        <div className="flex flex-col w-[360px] sm:w-[400px] h-[520px] rounded-2xl border border-slate-800 bg-slate-950 shadow-2xl overflow-hidden animate-in slide-in-from-bottom-5">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 bg-slate-900 border-b border-slate-800">
            <div className="flex items-center gap-2.5">
              <div className="relative">
                <div className="w-8 h-8 rounded-full bg-brand-600 flex items-center justify-center text-xs font-bold text-white">
                  CS
                </div>
                <span className="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-emerald-500 ring-2 ring-slate-900" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-white">Hỗ trợ B2B Trực tuyến</h4>
                <p className="text-[10px] text-slate-400">
                  {room?.assignedAdminName ? `Nhân viên: ${room.assignedAdminName}` : "Đội ngũ chuyên viên sỉ"}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-1">
              <button
                onClick={() => setIsOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
              >
                <Minimize2 className="w-4 h-4" />
              </button>
              <button
                onClick={() => setIsOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Message List */}
          <div className="flex-1 p-4 overflow-y-auto space-y-3 text-xs bg-slate-950/70">
            {messages.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center text-slate-500 space-y-2">
                <MessageSquare className="w-8 h-8 text-slate-700" />
                <p>Chưa có tin nhắn nào. Đặt câu hỏi về chính sách sỉ, hợp đồng hoặc đơn hàng!</p>
              </div>
            ) : (
              messages.map((msg) => {
                const isMe = msg.senderId === user?.id;
                return (
                  <div
                    key={msg.id}
                    className={cn("flex flex-col max-w-[80%]", isMe ? "ml-auto items-end" : "mr-auto items-start")}
                  >
                    {!isMe && (
                      <span className="text-[10px] text-slate-400 mb-1 font-medium">
                        {msg.senderName} ({msg.senderRole})
                      </span>
                    )}

                    <div
                      className={cn(
                        "rounded-2xl px-3.5 py-2.5 break-words leading-relaxed",
                        isMe
                          ? "bg-brand-600 text-white rounded-br-none shadow-sm shadow-brand-600/20"
                          : "bg-slate-900 text-slate-200 border border-slate-800 rounded-bl-none"
                      )}
                    >
                      {msg.messageType === "ORDER_REF" && (
                        <div className="mb-1 text-[10px] font-bold text-amber-300 flex items-center gap-1">
                          📦 Tham chiếu đơn hàng:
                        </div>
                      )}
                      <p>{msg.content}</p>
                    </div>

                    <div className="flex items-center gap-1 mt-1 text-[9px] text-slate-500">
                      <span>{formatDate(msg.createdAt)}</span>
                      {isMe && (
                        msg.isRead ? (
                          <CheckCheck className="w-3 h-3 text-sky-400" />
                        ) : (
                          <Check className="w-3 h-3 text-slate-500" />
                        )
                      )}
                    </div>
                  </div>
                );
              })
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Footer */}
          <form onSubmit={handleSendMessage} className="p-3 bg-slate-900 border-t border-slate-800 flex items-center gap-2">
            <input
              type="text"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              placeholder="Nhập nội dung cần hỗ trợ..."
              className="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-brand-500"
            />
            <Button type="submit" size="sm" variant="primary" className="h-8 px-3 rounded-xl shrink-0">
              <Send className="w-3.5 h-3.5" />
            </Button>
          </form>
        </div>
      )}
    </div>
  );
}
