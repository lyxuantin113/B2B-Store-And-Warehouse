import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { TokenStorage } from "./api-client";
import { ChatMessage, ChatReadReceiptEvent, SendMessageRequest } from "@/types/chat";

export class StompChatClient {
  private client: Client | null = null;
  private connected = false;
  private subscriptions = new Map<string, StompSubscription>();

  connect(onConnect?: () => void, onError?: (err: any) => void) {
    if (this.connected && this.client) {
      onConnect?.();
      return;
    }

    const token = TokenStorage.getAccessToken();
    const wsUrl = process.env.NEXT_PUBLIC_WS_URL || "http://localhost:8080/ws-chat";

    this.client = new Client({
      // Use SockJS factory for broad browser compatibility
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        Authorization: token ? `Bearer ${token}` : "",
      },
      debug: (str) => {
        if (process.env.NODE_ENV === "development") {
          console.debug("[STOMP]", str);
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true;
        onConnect?.();
      },
      onStompError: (frame) => {
        console.error("[STOMP Error]", frame.headers["message"], frame.body);
        onError?.(frame);
      },
      onWebSocketClose: () => {
        this.connected = false;
      },
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      this.client = null;
      this.connected = false;
    }
  }

  isConnected(): boolean {
    return this.connected;
  }

  subscribeRoomMessages(
    roomId: string,
    onMessage: (message: ChatMessage) => void
  ): () => void {
    const destination = `/topic/chat.room.${roomId}`;
    return this.subscribe(destination, (msg) => {
      try {
        const payload: ChatMessage = JSON.parse(msg.body);
        onMessage(payload);
      } catch (err) {
        console.error("Failed to parse room message", err);
      }
    });
  }

  subscribeRoomReadReceipts(
    roomId: string,
    onRead: (event: ChatReadReceiptEvent) => void
  ): () => void {
    const destination = `/topic/chat.room.${roomId}.read`;
    return this.subscribe(destination, (msg) => {
      try {
        const payload: ChatReadReceiptEvent = JSON.parse(msg.body);
        onRead(payload);
      } catch (err) {
        console.error("Failed to parse read receipt", err);
      }
    });
  }

  subscribeAdminNotifications(
    onNotification: (message: ChatMessage) => void
  ): () => void {
    const destination = `/topic/admin.chat.notifications`;
    return this.subscribe(destination, (msg) => {
      try {
        const payload: ChatMessage = JSON.parse(msg.body);
        onNotification(payload);
      } catch (err) {
        console.error("Failed to parse admin notification", err);
      }
    });
  }

  private subscribe(destination: string, callback: (message: IMessage) => void): () => void {
    if (!this.client || !this.connected) {
      console.warn("STOMP not connected yet. Cannot subscribe to", destination);
      return () => {};
    }

    // Unsubscribe existing subscription for the same destination if any
    if (this.subscriptions.has(destination)) {
      this.subscriptions.get(destination)?.unsubscribe();
    }

    const sub = this.client.subscribe(destination, callback);
    this.subscriptions.set(destination, sub);

    return () => {
      sub.unsubscribe();
      this.subscriptions.delete(destination);
    };
  }

  sendMessage(request: SendMessageRequest) {
    if (!this.client || !this.connected) {
      throw new Error("Chat WebSocket is not connected");
    }

    this.client.publish({
      destination: "/app/chat.send",
      body: JSON.stringify(request),
    });
  }

  sendMarkRead(roomId: string) {
    if (!this.client || !this.connected) return;

    this.client.publish({
      destination: "/app/chat.read",
      body: JSON.stringify({ roomId }),
    });
  }
}

// Global singleton instance for UI components
export const chatSocket = new StompChatClient();
export const getWebSocketClient = () => chatSocket;
