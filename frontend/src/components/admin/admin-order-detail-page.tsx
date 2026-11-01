"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { AdminOrderDetails } from "@/components/admin/admin-orders";
import { AdminShell } from "@/components/admin/admin-shell";
import { getApiErrorMessage } from "@/lib/api-error";
import { getAdminOrder } from "@/services/admin-service";
import { useAuthStore } from "@/stores/auth-store";
import type { AdminOrder } from "@/types/admin";

export function AdminOrderDetailPage({ orderId }: { orderId: string }) {
  const { user, token, hasHydrated } = useAuthStore();
  const ready = hasHydrated && Boolean(token) && user?.role === "admin";
  const [order, setOrder] = useState<AdminOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const id = Number(orderId);
  const load = useCallback(async () => {
    if (!Number.isSafeInteger(id) || id < 1) { setLoading(false); setError("Mã đơn hàng không hợp lệ."); return; }
    setLoading(true); setError(null);
    try { setOrder(await getAdminOrder(id)); }
    catch (cause) { setError(getApiErrorMessage(cause)); }
    finally { setLoading(false); }
  }, [id]);

  useEffect(() => {
    if (!ready) return;
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [ready, load]);

  return <AdminShell active="orders"><main className="admin-catalog-main admin-operations-main">
    <header className="admin-page-heading"><div><span className="eyebrow">Quản lý đơn hàng</span><h1>Chi tiết đơn hàng</h1><p>Xem sản phẩm, thanh toán và lịch sử xử lý đơn.</p></div><Link href="/admin/orders" className="secondary-button">Danh sách đơn hàng</Link></header>
    {error && <p className="catalog-notice error" role="alert">{error}</p>}
    {loading && !order && <div className="admin-empty">Đang tải chi tiết đơn hàng...</div>}
    {order && <div className="admin-list-card"><AdminOrderDetails order={order} onRefresh={load} /></div>}
  </main></AdminShell>;
}
