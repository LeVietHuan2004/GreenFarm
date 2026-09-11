"use client";

import { CheckCircle2, LoaderCircle, MapPin, PackageCheck, Truck, XCircle } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { RoleGuard } from "@/components/auth/role-guard";
import { SiteHeader } from "@/components/layout/site-header";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { formatDateTime, orderStatusLabel } from "@/lib/order-format";
import { deliveryOrderService } from "@/services/operations-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Order } from "@/types/order";

export function DeliveryOrdersDashboard() {
  const { token, hasHydrated } = useAuthStore();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = useCallback(async () => { setLoading(true); try { setOrders(await deliveryOrderService.findAll()); setError(null); } catch (cause) { setError(getApiErrorMessage(cause)); } finally { setLoading(false); } }, []);
  useEffect(() => { if (hasHydrated && token) void load(); }, [hasHydrated, token, load]);
  const apply = async (order: Order, action: "claim" | "out_for_delivery" | "delivered" | "delivery_failed") => {
    setBusy(order.id); setError(null); setMessage(null);
    try {
      const note = action === "delivery_failed" ? (window.prompt("Ly do giao that bai:") || "Giao hang khong thanh cong") : undefined;
      const updated = action === "claim" ? await deliveryOrderService.claim(order.id) : await deliveryOrderService.updateStatus(order.id, action, note);
      setOrders((items) => items.map((item) => item.id === order.id ? updated : item));
      setMessage(`Don #${order.id} da cap nhat: ${orderStatusLabel[updated.status] ?? updated.status}.`);
    } catch (cause) { setError(getApiErrorMessage(cause)); } finally { setBusy(null); }
  };
  return <RoleGuard allowedRoles={["delivery_staff"]} loginHref="/delivery/login">
    <div className="portal-shell"><SiteHeader />
      <main className="portal-main operations-main"><header className="portal-heading"><div><span className="eyebrow">Khu vuc giao hang</span><h1>Don duoc phan cong</h1><p>Nhan don, cap nhat hanh trinh va xac nhan ket qua giao hang.</p></div></header>
        {error && <p className="catalog-notice error">{error}</p>}{message && <p className="catalog-notice success">{message}</p>}
        <section className="operations-list">{loading && <div className="admin-empty"><LoaderCircle className="spin" size={20} /> Dang tai don giao...</div>}
          {!loading && orders.length === 0 && <div className="admin-empty"><Truck size={22} /> Chua co don duoc phan cong.</div>}
          {orders.map((order) => <article className="operation-card" key={order.id}><div className="operation-card-main"><div><strong>Don #{order.id}</strong><small>{formatDateTime(order.createdAt)}</small></div><span className={`order-status ${order.status}`}>{orderStatusLabel[order.status] ?? order.status}</span></div>
            <div className="operation-details"><span><MapPin size={15} /> {order.recipientName} · {order.recipientPhone}<br />{order.shippingAddress}, {order.shippingCity}</span><strong>{formatPrice(order.total)}</strong></div>
            <div className="operation-actions">{order.status === "ready_for_delivery" && <><button className="secondary-button" disabled={busy === order.id} onClick={() => void apply(order, "claim")}><PackageCheck size={16} /> Nhan don</button><button className="primary-button" disabled={busy === order.id} onClick={() => void apply(order, "out_for_delivery")}><Truck size={16} /> Bat dau giao</button></>}{order.status === "out_for_delivery" && <><button className="primary-button" disabled={busy === order.id} onClick={() => void apply(order, "delivered")}><CheckCircle2 size={16} /> Giao thanh cong</button><button className="secondary-button" disabled={busy === order.id} onClick={() => void apply(order, "delivery_failed")}><XCircle size={16} /> Giao that bai</button></>}</div>
          </article>)}</section>
      </main>
    </div>
  </RoleGuard>;
}
