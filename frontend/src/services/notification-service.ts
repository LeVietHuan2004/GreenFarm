import { apiClient } from "@/lib/axios-client";
import type { EngagementPage, EngagementResponse, Notification } from "@/types/engagement";

export async function getNotifications() { return (await apiClient.get<EngagementResponse<EngagementPage<Notification>>>("/notifications", { params: { page: 0, size: 20 } })).data.data; }
export async function getUnreadCount() { return (await apiClient.get<EngagementResponse<{unreadCount:number}>>("/notifications/unread-count")).data.data.unreadCount; }
export async function markNotificationRead(id: number) { return (await apiClient.patch<EngagementResponse<Notification>>(`/notifications/${id}/read`)).data.data; }
export async function markAllNotificationsRead() { await apiClient.patch("/notifications/read-all"); }
