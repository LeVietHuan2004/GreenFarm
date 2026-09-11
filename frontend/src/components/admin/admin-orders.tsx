"use client";

import { ChevronLeft, ChevronRight, MapPin, PackageCheck, ShoppingCart } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { AdminShell } from "@/components/admin/admin-shell";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { formatDateTime, orderStatusLabel } from "@/lib/order-format";
import { getAdminOrders, updateAdminOrderStatus } from "@/services/admin-service";
import { useAuthStore } from "@/stores/auth-store";
import type { AdminOrderStatus, AdminOrderSummary } from "@/types/admin";
import type { PageData } from "@/types/catalog";

const emptyPage: PageData<AdminOrderSummary> = {
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true
};

const statuses: AdminOrderStatus[] = ["pending", "processing", "ready_for_delivery", "out_for_delivery", "delivered", "completed", "canceled"];
const transitions: Record<AdminOrderStatus, AdminOrderStatus[]> = {
  pending: ["processing", "canceled"],
  processing: ["ready_for_delivery", "canceled"],
  ready_for_delivery: ["out_for_delivery", "canceled"],
  out_for_delivery: ["delivered"],
  delivered: ["completed"],
  completed: [],
  canceled: []
};

export function AdminOrders() {
  const { user, token, hasHydrated } = useAuthStore();
  const adminReady = hasHydrated && Boolean(token) && user?.role === "admin";
  const [orders, setOrders] = useState<PageData<AdminOrderSummary>>(emptyPage);
  const [status, setStatus] = useState<"" | AdminOrderStatus>("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadOrders = useCallback(async () => {
    setLoading(true);
    try {
      setOrders(await getAdminOrders({ status: status || undefined, page, size: 20, sort: "createdAt,desc" }));
      setError(null);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    if (!adminReady) return;
    const timer = window.setTimeout(() => { void loadOrders(); }, 0);
    return () => window.clearTimeout(timer);
  }, [adminReady, loadOrders]);

  const updateStatus = async (order: AdminOrderSummary, nextStatus: AdminOrderStatus) => {
    setUpdatingId(order.id);
    setMessage(null);
    setError(null);
    try {
      const updated = await updateAdminOrderStatus(order.id, nextStatus);
      setOrders((current) => ({
        ...current,
        content: current.content.map((item) => item.id === order.id ? { ...item, status: updated.status } : item)
      }));
      setMessage(`Đơn hàng #${order.id} đã chuyển sang “${orderStatusLabel[updated.status] ?? updated.status}”.`);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <AdminShell active="orders">
      <main className="admin-catalog-main admin-operations-main">
        <header className="admin-page-heading">
          <div><span className="eyebrow">Vận hành · Thanh toán</span><h1>Quản lý đơn hàng</h1><p>Cập nhật từng bước xử lý; COD được ghi nhận thanh toán khi giao thành công.</p></div>
          <span className="admin-count"><ShoppingCart size={15} /> {orders.totalElements} đơn hàng</span>
        </header>

        {error && <p className="catalog-notice error">{error}</p>}
        {message && <p className="catalog-notice success">{message}</p>}

        <section className="admin-list-card admin-operations-card">
          <div className="admin-operations-toolbar">
            <label>Trạng thái
              <select value={status} onChange={(event) => { setStatus(event.target.value as typeof status); setPage(0); }}>
                <option value="">Tất cả trạng thái</option>
                {statuses.map((item) => <option value={item} key={item}>{orderStatusLabel[item]}</option>)}
              </select>
            </label>
          </div>

          <div className="admin-order-list">
            {orders.content.map((order) => {
              const current = order.status as AdminOrderStatus;
              const available = transitions[current] ?? [];
              return (
                <article className="admin-order-row" key={order.id}>
                  <div className="admin-order-id"><span><PackageCheck size={19} /></span><div><strong>Đơn #{order.id}</strong><small>{formatDateTime(order.createdAt)}</small></div></div>
                  <div><strong>{order.recipientName}</strong><small><MapPin size={13} />{order.shippingCity}</small></div>
                  <div><strong>{formatPrice(order.total)}</strong><small>{order.itemCount} sản phẩm</small></div>
                  <span className={`order-status ${order.status}`}>{orderStatusLabel[order.status] ?? order.status}</span>
                  <label className="admin-order-action"><span>Bước tiếp theo</span>
                    <select
                      value=""
                      disabled={available.length === 0 || updatingId === order.id}
                      onChange={(event) => void updateStatus(order, event.target.value as AdminOrderStatus)}
                    >
                      <option value="">{updatingId === order.id ? "Đang cập nhật..." : available.length ? "Chọn trạng thái" : "Đã kết thúc"}</option>
                      {available.map((item) => <option value={item} key={item}>{orderStatusLabel[item]}</option>)}
                    </select>
                  </label>
                </article>
              );
            })}
            {loading && <div className="admin-empty">Đang tải đơn hàng...</div>}
            {!loading && orders.content.length === 0 && <div className="admin-empty">Không có đơn hàng phù hợp.</div>}
          </div>

          {orders.totalPages > 1 && <nav className="pagination admin-user-pagination" aria-label="Phân trang đơn hàng">
            <button type="button" disabled={orders.first || loading} onClick={() => setPage((value) => Math.max(0, value - 1))}><ChevronLeft size={17} /> Trước</button>
            <span>Trang {orders.page + 1} / {orders.totalPages}</span>
            <button type="button" disabled={orders.last || loading} onClick={() => setPage((value) => value + 1)}>Sau <ChevronRight size={17} /></button>
          </nav>}
        </section>
      </main>
    </AdminShell>
  );
}
