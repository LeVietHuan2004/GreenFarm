import { apiClient } from "@/lib/axios-client";
import type { EngagementPage, EngagementResponse, Notification } from "@/types/engagement";

export async function getNotifications() { return (await apiClient.get<EngagementResponse<EngagementPage<Notification>>>("/notifications", { params: { page: 0, size: 20 } })).data.data; }
export async function getUnreadCount() { return (await apiClient.get<EngagementResponse<{unreadCount:number}>>("/notifications/unread-count")).data.data.unreadCount; }
export async function markNotificationRead(id: number) { return (await apiClient.patch<EngagementResponse<Notification>>(`/notifications/${id}/read`)).data.data; }
export async function markAllNotificationsRead() { await apiClient.patch("/notifications/read-all"); }

export function subscribeToNotifications(token: string, onNotification: (value: Notification) => void) {
  const controller = new AbortController();
  const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
  void (async () => {
    try {
      const response = await fetch(`${baseUrl}/notifications/stream`, {
        headers: { Accept: "text/event-stream", Authorization: `Bearer ${token}` },
        signal: controller.signal,
      });
      if (!response.ok || !response.body) return;
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";
      while (!controller.signal.aborted) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split("\n\n");
        buffer = events.pop() ?? "";
        for (const event of events) {
          if (!event.includes("event:notification")) continue;
          const data = event.split("\n").find((line) => line.startsWith("data:"))?.slice(5);
          if (data) onNotification(JSON.parse(data) as Notification);
        }
      }
    } catch { /* Network interruption is recovered by the existing refresh/load cycle. */ }
  })();
  return () => controller.abort();
}
