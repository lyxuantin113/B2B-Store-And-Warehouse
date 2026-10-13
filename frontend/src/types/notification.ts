import { CursorPageDto } from "./api";

export interface NotificationItem {
  id: string;
  userId: string;
  notifType: string;
  title: string;
  message: string;
  referenceId?: string | null;
  isRead: boolean;
  readAt?: string | null;
  createdAt: string;
}

export interface AnnouncementItem {
  id: string;
  title: string;
  contentHtml: string;
  categoryBadge: string;
  isPinned: boolean;
  isPublished: boolean;
  publishedAt: string;
  createdAt: string;
}

export interface UnifiedNotificationCenter {
  unreadCount: number;
  personalNotifications: CursorPageDto<NotificationItem>;
  broadcastAnnouncements: AnnouncementItem[];
}
