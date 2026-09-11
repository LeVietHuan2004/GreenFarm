"use client";

import { ClipboardList, LoaderCircle, MapPin, PackageCheck, XCircle } from "lucide-react";
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
    try { setOrders(await staffOrderService.findAll()); setError(null); }
    catch (cause) { setError(getApiErrorMessage(cause)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { if (hasHydrated && token) void load(); }, [hasHydrated, token, load]);

  const update = async (order: Order, status: string, note?: string) => {
    setBusy(order.id); setError(null); setMessage(null);
    try {
      const updated = await staffOrderService.updateStatus(order.id, status, note);
      setOrders((items) => items.map((item) => item.id === order.id ? updated : item));
      setMessage(`Don #${order.id} da cap nhat: ${orderStatusLabel[updated.status] ?? updated.status}.`);
    } catch (cause) { setError(getApiErrorMessage(cause)); }
    finally { setBusy(null); }
  };

  return <RoleGuard allowedRoles={["staff"]} loginHref="/staff/login">
    <div className="portal-shell"><SiteHeader />
      <main className="portal-main operations-main">
        <header className="portal-heading"><div><span className="eyebrow">Van hanh don hang</span><h1>Don cho xu ly</h1><p>Xac nhan don moi, chuan bi hang va ban giao cho bo phan giao hang.</p></div></header>
        {error && <p className="catalog-notice error">{error}</p>}
        {message && <p className="catalog-notice success">{message}</p>}
        <section className="operations-list">
          {loading && <div className="admin-empty"><LoaderCircle className="spin" size={20} /> Dang tai don hang...</div>}
          {!loading && orders.length === 0 && <div className="admin-empty"><ClipboardList size={22} /> Khong co don can xu ly.</div>}
          {orders.map((order) => <article className="operation-card" key={order.id}>
            <div className="operation-card-main"><div><strong>Don #{order.id}</strong><small>{formatDateTime(order.createdAt)}</small></div><span className={`order-status ${order.status}`}>{orderStatusLabel[order.status] ?? order.status}</span></div>
            <div className="operation-details"><span><MapPin size={15} /> {order.recipientName} · {order.shippingCity}</span><strong>{formatPrice(order.total)}</strong></div>
            {order.deliveryFailureReason && <p className="operation-note">Ly do giao that bai: {order.deliveryFailureReason}</p>}
            <div className="operation-actions">
              {order.status === "pending" && <><button className="primary-button" disabled={busy === order.id} onClick={() => void update(order, "processing")}><PackageCheck size={16} /> Xac nhan</button><button className="secondary-button" disabled={busy === order.id} onClick={() => void update(order, "canceled", "Nhan vien tu choi don") }><XCircle size={16} /> Tu choi</button></>}
              {order.status === "processing" && <button className="primary-button" disabled={busy === order.id} onClick={() => void update(order, "ready_for_delivery")}><PackageCheck size={16} /> San sang giao</button>}
              {order.status === "delivery_failed" && <button className="primary-button" disabled={busy === order.id} onClick={() => void update(order, "ready_for_delivery", "Chuan bi giao lai")}><PackageCheck size={16} /> Dua lai cho giao hang</button>}
            </div>
          </article>)}
        </section>
      </main>
    </div>
  </RoleGuard>;
}
