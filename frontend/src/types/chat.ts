export type ChatRoomStatus = "OPEN" | "CLOSED";
export type ChatMessageType = "TEXT" | "IMAGE" | "ORDER_REF";

export interface ChatMessage {
  id: number;
  roomId: string;
  senderId: string;
  senderName: string;
  senderRole: string;
  messageType: ChatMessageType;
  content: string;
  isRead: boolean;
  createdAt: string;
}

export interface ChatRoom {
  id: string;
  partnerUserId: string;
  partnerName: string;
  partnerEmail: string;
  partnerCompanyName?: string | null;
  assignedAdminId?: string | null;
  assignedAdminName?: string | null;
  status: ChatRoomStatus;
  lastMessageAt: string;
  createdAt: string;
  unreadCount: number;
  lastMessagePreview?: string | null;
}

export interface SendMessageRequest {
  roomId?: string;
  messageType?: ChatMessageType;
  content: string;
}

export interface ChatReadReceiptEvent {
  roomId: string;
  readerUserId: string;
  markedCount: number;
  readAt: string;
}
