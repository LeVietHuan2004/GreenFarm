"use client";

import { ClipboardList, History, LoaderCircle, MapPin, PackageCheck, XCircle } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { RoleGuard } from "@/components/auth/role-guard";
import { SiteHeader } from "@/components/layout/site-header";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { formatDateTime, orderStatusLabel } from "@/lib/order-format";
import { staffOrderService } from "@/services/operations-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Order } from "@/types/order";

export function StaffOrdersDashboard() {
  const { token, hasHydrated } = useAuthStore();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setOrders(await staffOrderService.findAll());
      setError(null);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!hasHydrated || !token) return;
    const timer = window.setTimeout(() => { void load(); }, 0);
    return () => window.clearTimeout(timer);
  }, [hasHydrated, token, load]);

  const update = async (order: Order, status: string, note?: string) => {
    setBusy(order.id);
    setError(null);
    setMessage(null);
    try {
      const updated = await staffOrderService.updateStatus(order.id, status, note);
      setOrders((items) => items.map((item) => item.id === order.id ? updated : item));
      setMessage(`Đơn #${order.id} đã cập nhật: ${orderStatusLabel[updated.status] ?? updated.status}.`);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setBusy(null);
    }
  };

  return (
    <RoleGuard allowedRoles={["staff"]} loginHref="/staff/login">
      <div className="portal-shell">
        <SiteHeader />
        <main className="portal-main operations-main">
          <header className="portal-heading">
            <div>
              <span className="eyebrow">Vận hành đơn hàng</span>
              <h1>Đơn chờ xử lý</h1>
              <p>Xác nhận đơn mới, chuẩn bị hàng và bàn giao cho bộ phận giao hàng.</p>
            </div>
            <Link className="secondary-button" href="/staff/contacts">Hỗ trợ khách hàng</Link>
          </header>

          {error && <p className="catalog-notice error">{error}</p>}
          {message && <p className="catalog-notice success">{message}</p>}

          <section className="operations-list">
            {loading && <div className="admin-empty"><LoaderCircle className="spin" size={20} /> Đang tải đơn hàng...</div>}
            {!loading && orders.length === 0 && <div className="admin-empty"><ClipboardList size={22} /> Không có đơn cần xử lý.</div>}
            {orders.map((order) => (
              <article className="operation-card" key={order.id}>
                <div className="operation-card-main">
                  <div><strong>Đơn #{order.id}</strong><small>{formatDateTime(order.createdAt)}</small></div>
                  <span className={`order-status ${order.status}`}>{orderStatusLabel[order.status] ?? order.status}</span>
                </div>
                <div className="operation-details">
                  <span><MapPin size={15} /> {order.recipientName} · {order.shippingCity}</span>
                  <strong>{formatPrice(order.total)}</strong>
                </div>
                {order.deliveryStaffName && <p className="operation-note">Đã phân công: {order.deliveryStaffName}{order.deliveryClaimedAt ? " · Đã nhận đơn" : " · Chờ nhận đơn"}</p>}
                {order.deliveryFailureReason && <p className="operation-note">Lý do giao thất bại: {order.deliveryFailureReason}</p>}
                <details className="operation-history">
                  <summary><History size={15} /> Lịch sử xử lý ({order.statusHistory.length})</summary>
                  <ol>
                    {order.statusHistory.map((item) => (
                      <li key={item.id}>
                        <span>{orderStatusLabel[item.status] ?? item.status}</span>
                        <small>{item.note || "Không có ghi chú"} · {formatDateTime(item.changedAt)}</small>
                      </li>
                    ))}
                  </ol>
                </details>
                <div className="operation-actions">
                  {order.status === "pending" && <>
                    <button type="button" className="primary-button" disabled={busy === order.id} onClick={() => void update(order, "processing")}><PackageCheck size={16} /> Xác nhận</button>
                    <button type="button" className="secondary-button" disabled={busy === order.id} onClick={() => void update(order, "canceled", "Nhân viên từ chối đơn")}><XCircle size={16} /> Từ chối</button>
                  </>}
                  {order.status === "processing" && <button type="button" className="primary-button" disabled={busy === order.id} onClick={() => void update(order, "ready_for_delivery")}><PackageCheck size={16} /> Sẵn sàng giao</button>}
                  {order.status === "delivery_failed" && <button type="button" className="primary-button" disabled={busy === order.id} onClick={() => void update(order, "ready_for_delivery", "Chuẩn bị giao lại")}><PackageCheck size={16} /> Đưa lại cho giao hàng</button>}
                </div>
              </article>
            ))}
          </section>
        </main>
      </div>
    </RoleGuard>
  );
}
