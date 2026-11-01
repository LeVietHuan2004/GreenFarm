"use client";

import { ChevronLeft, ChevronRight, Search, Truck } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { AdminShell } from "@/components/admin/admin-shell";
import { GhnShipmentPanel } from "@/components/admin/admin-orders";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { ghnStatusLabels } from "@/lib/ghn-status";
import { formatDateTime } from "@/lib/order-format";
import { getAdminOrder, getAdminShipments, type AdminShipment } from "@/services/admin-service";
import { useAuthStore } from "@/stores/auth-store";
import type { AdminOrder } from "@/types/admin";
import type { PageData } from "@/types/catalog";

const emptyPage: PageData<AdminShipment> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true };

export function AdminDelivery() {
  const { user, token, hasHydrated } = useAuthStore();
  const ready = hasHydrated && Boolean(token) && user?.role === "admin";
  const [input, setInput] = useState("");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [shipments, setShipments] = useState(emptyPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<number | null>(null);
  const [detail, setDetail] = useState<AdminOrder | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const requestId = useRef(0);
  const detailRequestId = useRef(0);

  const load = useCallback(async () => {
    const id = ++requestId.current;
    setLoading(true);
    try {
      const result = await getAdminShipments({ search: search || undefined, status: status || undefined, page, size: 20 });
      if (id !== requestId.current) return;
      if (page > 0 && result.content.length === 0 && result.totalPages > 0) { setPage(result.totalPages - 1); return; }
      setShipments(result); setError(null);
    } catch (cause) { if (id === requestId.current) setError(getApiErrorMessage(cause)); }
    finally { if (id === requestId.current) setLoading(false); }
  }, [page, search, status]);

  useEffect(() => {
    if (!ready) return;
    const timer = window.setTimeout(() => void load(), 0);
    return () => { window.clearTimeout(timer); requestId.current += 1; };
  }, [ready, load]);

  const fetchDetail = async (orderId: number) => {
    const id = ++detailRequestId.current;
    setDetailLoading(true); setDetailError(null);
    try { const result = await getAdminOrder(orderId); if (id === detailRequestId.current) setDetail(result); }
    catch (cause) { if (id === detailRequestId.current) setDetailError(getApiErrorMessage(cause)); }
    finally { if (id === detailRequestId.current) setDetailLoading(false); }
  };
  const closeDetail = () => { detailRequestId.current += 1; setExpanded(null); setDetail(null); setDetailError(null); };
  const toggleDetail = (orderId: number) => {
    if (expanded === orderId) { closeDetail(); return; }
    setExpanded(orderId); setDetail(null); void fetchDetail(orderId);
  };

  return <AdminShell active="delivery">
    <main className="admin-catalog-main admin-operations-main">
      <header className="admin-page-heading">
        <div><span className="eyebrow">Vận hành · GHN</span><h1>Quản lý giao hàng</h1><p>Theo dõi vận đơn GHN và xử lý vận chuyển theo từng đơn hàng.</p></div>
        <span className="admin-count"><Truck size={15} /> {shipments.totalElements} vận đơn</span>
      </header>
      {error && <p className="catalog-notice error" role="alert">{error}</p>}
      <section className="admin-list-card admin-operations-card">
        <div className="delivery-toolbar">
          <form onSubmit={(event) => { event.preventDefault(); closeDetail(); setPage(0); setSearch(input.trim()); }}>
            <label htmlFor="delivery-search">Tìm vận đơn hoặc đơn hàng</label>
            <div><input id="delivery-search" value={input} onChange={(event) => setInput(event.target.value)} placeholder="Mã vận đơn, mã đơn, tên hoặc số điện thoại" /><button type="submit" className="primary-button"><Search size={16} /> Tìm</button></div>
          </form>
          <label htmlFor="delivery-status">Trạng thái vận chuyển
            <select id="delivery-status" value={status} onChange={(event) => { closeDetail(); setPage(0); setStatus(event.target.value); }}>
              <option value="">Tất cả trạng thái</option>{Object.entries(ghnStatusLabels).map(([key, label]) => <option value={key} key={key}>{label}</option>)}
            </select>
          </label>
        </div>
        {loading && <div className="admin-empty">Đang tải vận đơn...</div>}
        {!loading && shipments.content.length === 0 && !error && <div className="admin-empty">Không có vận đơn phù hợp.</div>}
        {shipments.content.map((shipment) => <article className="delivery-entry" key={shipment.orderId}>
          <button type="button" className="delivery-summary" aria-expanded={expanded === shipment.orderId} onClick={() => toggleDetail(shipment.orderId)}>
            <span><strong>Đơn #{shipment.orderId}</strong><small>{shipment.createdAt ? formatDateTime(shipment.createdAt) : "—"}</small></span>
            <span><strong>{shipment.providerOrderCode ?? "Chưa tạo vận đơn"}</strong><small>{ghnStatusLabels[shipment.shippingStatus] ?? shipment.shippingStatus}</small></span>
            <span><strong>{shipment.recipientName}</strong><small>{shipment.recipientPhone} · {shipment.shippingCity}</small></span>
            <span><strong>{shipment.carrierShippingFee === null ? "Chưa có phí GHN" : formatPrice(shipment.carrierShippingFee)}</strong><small>GHN thu hộ: {formatPrice(shipment.codAmount)}</small></span>
            <ChevronRight size={18} aria-hidden="true" />
          </button>
          {expanded === shipment.orderId && <div className="admin-order-detail" aria-live="polite">
            {detailLoading && <p>Đang tải chi tiết đơn hàng...</p>}
            {detailError && <div className="catalog-notice error">{detailError} <button type="button" onClick={() => void fetchDetail(shipment.orderId)}>Thử lại</button></div>}
            {detail?.id === shipment.orderId && <div className="delivery-shipment-detail">
              <div className="delivery-recipient"><div><strong>Người nhận</strong><p>{detail.recipientName} · {detail.recipientPhone}</p><p>{detail.shippingAddress}, {detail.shippingCity}</p></div><Link href={`/admin/orders/${detail.id}`}>Xem chi tiết đơn #{detail.id} <ChevronRight size={15} /></Link></div>
              <GhnShipmentPanel order={detail} onRefresh={async () => { await fetchDetail(shipment.orderId); await load(); }} />
            </div>}
          </div>}
        </article>)}
        {shipments.totalPages > 1 && <nav className="pagination admin-user-pagination" aria-label="Phân trang vận đơn">
          <button type="button" disabled={shipments.first || loading} onClick={() => { closeDetail(); setPage(value => value - 1); }}><ChevronLeft size={17} /> Trước</button>
          <span>Trang {shipments.page + 1} / {shipments.totalPages}</span>
          <button type="button" disabled={shipments.last || loading} onClick={() => { closeDetail(); setPage(value => value + 1); }}>Sau <ChevronRight size={17} /></button>
        </nav>}
      </section>
    </main>
  </AdminShell>;
}
