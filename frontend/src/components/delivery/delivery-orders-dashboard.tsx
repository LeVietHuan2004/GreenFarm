"use client";

import { CheckCircle2, History, LoaderCircle, MapPin, PackageCheck, Truck, XCircle } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { RoleGuard } from "@/components/auth/role-guard";
import { SiteHeader } from "@/components/layout/site-header";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { formatDateTime, orderStatusLabel } from "@/lib/order-format";
import { deliveryOrderService } from "@/services/operations-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Order } from "@/types/order";

type DeliveryAction = "claim" | "out_for_delivery" | "delivered" | "delivery_failed";

export function DeliveryOrdersDashboard() {
  const { token, hasHydrated } = useAuthStore();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setOrders(await deliveryOrderService.findAll());
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

  const apply = async (order: Order, action: DeliveryAction) => {
    const failureReason = action === "delivery_failed" ? window.prompt("Lý do giao thất bại:") : null;
    if (action === "delivery_failed" && failureReason === null) return;

    setBusy(order.id);
    setError(null);
    setMessage(null);
    try {
      const note = action === "delivery_failed" ? (failureReason?.trim() || "Giao hàng không thành công") : undefined;
      const updated = action === "claim"
        ? await deliveryOrderService.claim(order.id)
        : await deliveryOrderService.updateStatus(order.id, action, note);
      setOrders((items) => items.map((item) => item.id === order.id ? updated : item));
      setMessage(`Đơn #${order.id} đã cập nhật: ${orderStatusLabel[updated.status] ?? updated.status}.`);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setBusy(null);
    }
  };

  return (
    <RoleGuard allowedRoles={["delivery_staff"]} loginHref="/delivery/login">
      <div className="portal-shell">
        <SiteHeader />
        <main className="portal-main operations-main">
          <header className="portal-heading">
            <div>
              <span className="eyebrow">Khu vực giao hàng</span>
              <h1>Đơn được phân công</h1>
              <p>Nhận đơn, cập nhật hành trình và xác nhận kết quả giao hàng.</p>
            </div>
          </header>

          {error && <p className="catalog-notice error">{error}</p>}
          {message && <p className="catalog-notice success">{message}</p>}

          <section className="operations-list">
            {loading && <div className="admin-empty"><LoaderCircle className="spin" size={20} /> Đang tải đơn giao...</div>}
            {!loading && orders.length === 0 && <div className="admin-empty"><Truck size={22} /> Chưa có đơn được phân công.</div>}
            {orders.map((order) => (
              <article className="operation-card" key={order.id}>
                <div className="operation-card-main">
                  <div><strong>Đơn #{order.id}</strong><small>{formatDateTime(order.createdAt)}</small></div>
                  <span className={`order-status ${order.status}`}>{orderStatusLabel[order.status] ?? order.status}</span>
                </div>
                <div className="operation-details">
                  <span><MapPin size={15} /> {order.recipientName} · {order.recipientPhone}<br />{order.shippingAddress}, {order.shippingCity}</span>
                  <strong>{formatPrice(order.total)}</strong>
                </div>
                {order.deliveryClaimedAt && <p className="operation-note success-text">Đã nhận đơn lúc {formatDateTime(order.deliveryClaimedAt)}</p>}
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
                  {order.status === "ready_for_delivery" && !order.deliveryClaimedAt && <button type="button" className="secondary-button" disabled={busy === order.id} onClick={() => void apply(order, "claim")}><PackageCheck size={16} /> Nhận đơn</button>}
                  {order.status === "ready_for_delivery" && order.deliveryClaimedAt && <button type="button" className="primary-button" disabled={busy === order.id} onClick={() => void apply(order, "out_for_delivery")}><Truck size={16} /> Bắt đầu giao</button>}
                  {order.status === "out_for_delivery" && <>
                    <button type="button" className="primary-button" disabled={busy === order.id} onClick={() => void apply(order, "delivered")}><CheckCircle2 size={16} /> Giao thành công</button>
                    <button type="button" className="secondary-button" disabled={busy === order.id} onClick={() => void apply(order, "delivery_failed")}><XCircle size={16} /> Giao thất bại</button>
                  </>}
                </div>
              </article>
            ))}
          </section>
        </main>
      </div>
    </RoleGuard>
  );
}
