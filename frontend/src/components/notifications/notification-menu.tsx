"use client";

import { Bell, CheckCheck, MessageCircle, PackageCheck, Truck } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { getApiErrorMessage } from "@/lib/api-error";
import { formatDateTime } from "@/lib/order-format";
import { getNotifications, getUnreadCount, markAllNotificationsRead, markNotificationRead } from "@/services/notification-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Notification } from "@/types/engagement";

function NotificationIcon({ type }: { type: string }) {
  if (type === "delivery") return <Truck size={16} />;
  if (type === "contact") return <MessageCircle size={16} />;
  return <PackageCheck size={16} />;
}

export function NotificationMenu({ admin = false }: { admin?: boolean }) {
  const { user, hasHydrated } = useAuthStore();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<Notification[]>([]);
  const [unread, setUnread] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [page, unreadCount] = await Promise.all([getNotifications(), getUnreadCount()]);
      setItems(page.content);
      setUnread(unreadCount);
      setError(null);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    }
  }, []);

  useEffect(() => {
    if (!hasHydrated || !user) return;
    const timer = window.setTimeout(() => { void load(); }, 0);
    return () => window.clearTimeout(timer);
  }, [hasHydrated, user, load]);

  const read = async (id: number) => {
    setItems((current) => current.map((item) => item.id === id ? { ...item, read: true } : item));
    setUnread((current) => Math.max(0, current - 1));
    try { await markNotificationRead(id); } catch { void load(); }
  };
  const readAll = async () => {
    setItems((current) => current.map((item) => ({ ...item, read: true })));
    setUnread(0);
    try { await markAllNotificationsRead(); } catch { void load(); }
  };

  if (!hasHydrated || !user) return null;

  return <div className={`notification-menu${admin ? " admin" : ""}`}>
    <button type="button" className={admin ? "admin-topbar-icon" : "header-icon-link"} onClick={() => setOpen((value) => !value)} aria-label={`Thông báo (${unread} chưa đọc)`} aria-expanded={open}>
      <Bell size={18} />{unread > 0 && <span className="notification-count">{unread > 99 ? "99+" : unread}</span>}
    </button>
    {open && <div className="notification-dropdown">
      <header><div><strong>Thông báo</strong><small>{unread} chưa đọc</small></div>{unread > 0 && <button type="button" onClick={() => void readAll()}><CheckCheck size={15} /> Đọc tất cả</button>}</header>
      <div className="notification-list">
        {error && <p className="notification-empty error">{error}</p>}
        {!error && items.length === 0 && <p className="notification-empty">Chưa có thông báo.</p>}
        {items.map((item) => <Link href={item.link || "#"} className={item.read ? "" : "unread"} onClick={() => { setOpen(false); if (!item.read) void read(item.id); }} key={item.id}>
          <span className={`notification-type ${item.type}`}><NotificationIcon type={item.type} /></span>
          <span><strong>{item.message}</strong><small>{formatDateTime(item.createdAt)}</small></span>
        </Link>)}
      </div>
    </div>}
  </div>;
}
