"use client";

import { Check, ChevronLeft, ChevronRight, MapPin, PackageCheck, ShoppingCart, X } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";

import { AdminShell } from "@/components/admin/admin-shell";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { formatDateTime, orderStatusLabel } from "@/lib/order-format";
import { assignAdminOrderDelivery, decideAdminRefundRequest, getAdminOrder, getAdminOrders, getAdminRefundRequests, getAdminUsers, updateAdminOrderStatus } from "@/services/admin-service";
import { useAuthStore } from "@/stores/auth-store";
import type { AdminOrder, AdminOrderStatus, AdminOrderSummary, AdminOrderView, AdminRefundRequest } from "@/types/admin";
import type { PageData } from "@/types/catalog";
import type { User } from "@/types/auth";

const emptyPage: PageData<AdminOrderSummary> = {
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true
};

const statuses: AdminOrderStatus[] = ["pending", "processing", "ready_for_delivery", "out_for_delivery", "delivered", "delivery_failed", "completed", "canceled"];
const activeStatuses = statuses.filter((status) => status !== "completed" && status !== "canceled");
const historyStatuses: AdminOrderStatus[] = ["completed", "canceled"];
const views: { value: AdminOrderView; label: string }[] = [
  { value: "active", label: "Cần xử lý" },
  { value: "history", label: "Lịch sử" },
  { value: "all", label: "Tất cả" }
];
const transitions: Record<AdminOrderStatus, AdminOrderStatus[]> = {
  pending: ["processing", "canceled"],
  processing: ["ready_for_delivery", "canceled"],
  ready_for_delivery: ["out_for_delivery", "canceled"],
  out_for_delivery: ["delivered"],
  delivered: ["completed"],
  delivery_failed: ["ready_for_delivery", "canceled"],
  completed: [],
  canceled: []
};

export function AdminOrders() {
  const { user, token, hasHydrated } = useAuthStore();
  const adminReady = hasHydrated && Boolean(token) && user?.role === "admin";
  const [orders, setOrders] = useState<PageData<AdminOrderSummary>>(emptyPage);
  const [deliveryStaff, setDeliveryStaff] = useState<User[]>([]);
  const [view, setView] = useState<AdminOrderView>("active");
  const [status, setStatus] = useState<"" | AdminOrderStatus>("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [refundRequests, setRefundRequests] = useState<AdminRefundRequest[]>([]);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [orderDetails, setOrderDetails] = useState<Record<number, AdminOrder>>({});
  const [detailLoadingId, setDetailLoadingId] = useState<number | null>(null);
  const [detailError, setDetailError] = useState<{ id: number; message: string } | null>(null);
  const loadRequestId = useRef(0);

  const loadOrders = useCallback(async () => {
    const requestId = ++loadRequestId.current;
    setLoading(true);
    try {
      const [orderPage, staffPage, refundPage] = await Promise.all([
        getAdminOrders({ view, status: status || undefined, page, size: 20, sort: "createdAt,desc" }),
        getAdminUsers({ role: "delivery_staff", status: "active", page: 0, size: 100 }),
        getAdminRefundRequests("pending")
      ]);
      if (requestId !== loadRequestId.current) return;
      if (page > 0 && orderPage.content.length === 0 && orderPage.totalPages > 0) {
        setPage(orderPage.totalPages - 1);
        return;
      }
      setOrders(orderPage);
      setDeliveryStaff(staffPage.content);
      setRefundRequests(refundPage.content);
      setError(null);
    } catch (requestError) {
      if (requestId === loadRequestId.current) setError(getApiErrorMessage(requestError));
    } finally {
      if (requestId === loadRequestId.current) setLoading(false);
    }
  }, [page, status, view]);

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
      setOrderDetails((current) => ({ ...current, [order.id]: updated }));
      await loadOrders();
      setMessage(`Đơn hàng #${order.id} đã chuyển sang “${orderStatusLabel[updated.status] ?? updated.status}”.`);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setUpdatingId(null);
    }
  };

  const assignDelivery = async (order: AdminOrderSummary, deliveryStaffId: number) => {
    setUpdatingId(order.id); setMessage(null); setError(null);
    try {
      const updated = await assignAdminOrderDelivery(order.id, deliveryStaffId);
      setOrders((current) => ({
        ...current,
        content: current.content.map((item) => item.id === order.id ? {
          ...item,
          deliveryStaffId: updated.deliveryStaffId,
          deliveryStaffName: updated.deliveryStaffName,
          deliveryClaimedAt: updated.deliveryClaimedAt
        } : item)
      }));
      setOrderDetails((current) => ({ ...current, [order.id]: updated }));
      setMessage(`Don hang #${order.id} da duoc phan cong giao hang.`);
    } catch (requestError) { setError(getApiErrorMessage(requestError)); }
    finally { setUpdatingId(null); }
  };

  const decideRefund = async (request: AdminRefundRequest, decision: "approve" | "reject") => {
    const note = window.prompt(decision === "approve" ? "Ghi chú duyệt hoàn tiền (không bắt buộc):" : "Lý do từ chối (khuyến nghị nhập):") ?? undefined;
    if (note === undefined) return;
    setUpdatingId(request.id); setError(null); setMessage(null);
    try { const updated = await decideAdminRefundRequest(request.id, decision, note); setRefundRequests((items) => items.filter((item) => item.id !== updated.id)); setOrderDetails((current) => { const next = { ...current }; delete next[request.orderId]; return next; }); setExpandedId((current) => current === request.orderId ? null : current); setMessage(`Yêu cầu hoàn tiền đơn #${request.orderId} đã được ${decision === "approve" ? "duyệt" : "từ chối"}.`); await loadOrders(); }
    catch (requestError) { setError(getApiErrorMessage(requestError)); }
    finally { setUpdatingId(null); }
  };

  const fetchDetails = async (orderId: number) => {
    setDetailError(null);
    setDetailLoadingId(orderId);
    try {
      const detail = await getAdminOrder(orderId);
      setOrderDetails((current) => ({ ...current, [orderId]: detail }));
    } catch (requestError) {
      setDetailError({ id: orderId, message: getApiErrorMessage(requestError) });
    } finally {
      setDetailLoadingId((current) => current === orderId ? null : current);
    }
  };

  const toggleDetails = async (orderId: number) => {
    if (expandedId === orderId) { setExpandedId(null); return; }
    setExpandedId(orderId);
    setDetailError(null);
    if (!orderDetails[orderId]) await fetchDetails(orderId);
  };

  return (
    <AdminShell active="orders">
      <main className="admin-catalog-main admin-operations-main">
        <header className="admin-page-heading">
          <div><span className="eyebrow">Vận hành · Thanh toán</span><h1>Quản lý đơn hàng</h1><p>Cập nhật từng bước xử lý; COD được ghi nhận thanh toán khi giao thành công.</p></div>
          <span className="admin-count"><ShoppingCart size={15} /> {orders.totalElements} {view === "active" ? "đơn cần xử lý" : view === "history" ? "đơn trong lịch sử" : "đơn hàng"}</span>
        </header>

        {error && <p className="catalog-notice error">{error}</p>}
        {message && <p className="catalog-notice success">{message}</p>}

        {refundRequests.length > 0 && <section className="admin-list-card admin-refund-queue"><div className="admin-refund-heading"><div><strong>Yêu cầu hoàn tiền chờ duyệt</strong><small>Chỉ duyệt khi đủ điều kiện; hệ thống sẽ gửi lệnh hoàn tiền VNPAY.</small></div><span>{refundRequests.length} yêu cầu</span></div>{refundRequests.map((request) => <article className="admin-refund-row" key={request.id}><div><strong>Đơn #{request.orderId} · {request.customerName}</strong><small>{request.reason}</small>{request.details && <small>Ghi chú: {request.details}</small>}</div><small>{formatDateTime(request.createdAt)}</small><div><button type="button" className="refund-approve" disabled={updatingId === request.id} onClick={() => void decideRefund(request, "approve")}><Check size={15}/>Duyệt hoàn</button><button type="button" className="refund-reject" disabled={updatingId === request.id} onClick={() => void decideRefund(request, "reject")}><X size={15}/>Từ chối</button></div></article>)}</section>}

        <section className="admin-list-card admin-operations-card">
          <div className="admin-operations-toolbar">
            <div className="admin-order-views" role="group" aria-label="Nhóm đơn hàng">
              {views.map((option) => <button type="button" key={option.value} aria-pressed={view === option.value} onClick={() => {
                if (view === option.value) return;
                loadRequestId.current += 1;
                setView(option.value); setStatus(""); setPage(0); setExpandedId(null); setOrders(emptyPage); setLoading(true);
              }}>{option.label}</button>)}
            </div>
            <label>Trạng thái
              <select value={status} onChange={(event) => { loadRequestId.current += 1; setStatus(event.target.value as typeof status); setPage(0); setExpandedId(null); setOrders(emptyPage); setLoading(true); }}>
                <option value="">Tất cả trạng thái</option>
                {(view === "active" ? activeStatuses : view === "history" ? historyStatuses : statuses).map((item) => <option value={item} key={item}>{orderStatusLabel[item]}</option>)}
              </select>
            </label>
          </div>

          <div className="admin-order-list">
            {orders.content.map((order) => {
              const current = order.status as AdminOrderStatus;
              const closed = current === "completed" || current === "canceled";
              const available: AdminOrderStatus[] = current === "ready_for_delivery" && !order.deliveryClaimedAt
                ? ["canceled"]
                : transitions[current] ?? [];
              return (
                <article className={`admin-order-entry ${closed ? "closed" : ""} ${expandedId === order.id ? "expanded" : ""}`} key={order.id}>
                  <div className="admin-order-row">
                    <button type="button" className="admin-order-summary" aria-expanded={expandedId === order.id} aria-controls={`admin-order-detail-${order.id}`} onClick={() => void toggleDetails(order.id)}>
                      <span className="admin-order-id"><span><PackageCheck size={19} /></span><span><strong>Đơn #{order.id}</strong><small>{formatDateTime(order.createdAt)}</small></span></span>
                      <span className="admin-order-person"><strong>{order.recipientName}</strong><small><MapPin size={13} />{order.shippingCity}</small></span>
                      <span className="admin-order-price"><strong>{formatPrice(order.total)}</strong><small>{order.itemCount} sản phẩm</small></span>
                      <span className="admin-order-status-cell"><span className={`order-status ${order.status}`}>{orderStatusLabel[order.status] ?? order.status}</span><ChevronRight size={17} aria-hidden="true" /></span>
                    </button>
                    {!closed && <label className="admin-order-action"><span>Giao hàng</span>
                      <select value={order.deliveryStaffId ?? ""} disabled={order.status !== "ready_for_delivery" || updatingId === order.id} onChange={(event) => { if (event.target.value) void assignDelivery(order, Number(event.target.value)); }}>
                        <option value="">{order.deliveryStaffName ?? "Chưa phân công"}</option>
                        {deliveryStaff.map((staff) => <option key={staff.id} value={staff.id}>{staff.name}</option>)}
                      </select>
                      {order.deliveryStaffId && <small>{order.deliveryClaimedAt ? "Đã nhận đơn" : "Chờ nhận đơn"}</small>}
                    </label>}
                    {!closed && <label className="admin-order-action"><span>Bước tiếp theo</span>
                      <select value="" disabled={available.length === 0 || updatingId === order.id} onChange={(event) => void updateStatus(order, event.target.value as AdminOrderStatus)}>
                        <option value="">{updatingId === order.id ? "Đang cập nhật..." : available.length ? "Chọn trạng thái" : "Đã kết thúc"}</option>
                        {available.map((item) => <option value={item} key={item}>{orderStatusLabel[item]}</option>)}
                      </select>
                    </label>}
                  </div>
                  {expandedId === order.id && <div id={`admin-order-detail-${order.id}`} className="admin-order-detail" aria-live="polite">
                    {detailLoadingId === order.id && <p>Đang tải chi tiết đơn hàng...</p>}
                    {detailError?.id === order.id && <div className="catalog-notice error">{detailError.message}<button type="button" onClick={() => void fetchDetails(order.id)}>Thử lại</button></div>}
                    {orderDetails[order.id] && <AdminOrderDetails order={orderDetails[order.id]} />}
                  </div>}
                </article>
              );
            })}
            {loading && <div className="admin-empty">Đang tải đơn hàng...</div>}
            {!loading && orders.content.length === 0 && <div className="admin-empty">{view === "active" && !status ? "Không có đơn cần xử lý." : "Không có đơn hàng phù hợp."}</div>}
          </div>

          {orders.totalPages > 1 && <nav className="pagination admin-user-pagination" aria-label="Phân trang đơn hàng">
            <button type="button" disabled={orders.first || loading} onClick={() => { loadRequestId.current += 1; setPage((value) => Math.max(0, value - 1)); setExpandedId(null); }}><ChevronLeft size={17} /> Trước</button>
            <span>Trang {orders.page + 1} / {orders.totalPages}</span>
            <button type="button" disabled={orders.last || loading} onClick={() => { loadRequestId.current += 1; setPage((value) => value + 1); setExpandedId(null); }}>Sau <ChevronRight size={17} /></button>
          </nav>}
        </section>
      </main>
    </AdminShell>
  );
}

function AdminOrderDetails({ order }: { order: AdminOrder }) {
  const paymentStatus: Record<string, string> = { pending: "Chờ thanh toán", completed: "Đã thanh toán", failed: "Thất bại", refunded: "Đã hoàn tiền" };
  return (
    <div>
      <div className="admin-order-detail-heading"><h3>Chi tiết đơn #{order.id}</h3><span className={`order-status ${order.status}`}>{orderStatusLabel[order.status] ?? order.status}</span></div>
      <div className="admin-order-detail-grid">
        <section className="admin-order-detail-card">
          <h4>Sản phẩm</h4>
          <div className="admin-order-detail-items">{order.items.map((item) => <div key={item.id}>
            <span><strong>{item.productName}</strong><small>{item.quantity} {item.productUnit ?? "sản phẩm"} × {formatPrice(item.unitPrice)}</small></span>
            <strong>{formatPrice(item.lineTotal)}</strong>
          </div>)}</div>
          <div className="admin-order-detail-totals">
            <div><span>Tạm tính</span><strong>{formatPrice(order.subtotal)}</strong></div>
            <div><span>Phí giao hàng</span><strong>{formatPrice(order.shippingFee)}</strong></div>
            {order.discountAmount > 0 && <div><span>Giảm trên đơn{order.couponCode ? ` · ${order.couponCode}` : ""}</span><strong>−{formatPrice(order.discountAmount)}</strong></div>}
            {order.shippingDiscountAmount > 0 && <div><span>Giảm phí giao hàng{order.shippingCouponCode ? ` · ${order.shippingCouponCode}` : ""}</span><strong>−{formatPrice(order.shippingDiscountAmount)}</strong></div>}
            {order.loyaltyDiscountAmount > 0 && <div><span>Điểm thưởng ({order.loyaltyPointsUsed} điểm)</span><strong>−{formatPrice(order.loyaltyDiscountAmount)}</strong></div>}
            <div className="admin-order-detail-total"><span>Tổng cộng</span><strong>{formatPrice(order.total)}</strong></div>
          </div>
        </section>
        <div className="admin-order-detail-side">
          <section className="admin-order-detail-card"><h4>Giao hàng</h4><p><strong>{order.recipientName}</strong> · {order.recipientPhone}</p><p>{order.shippingAddress}, {order.shippingCity}</p>{order.deliveryStaffName && <p>Nhân viên giao: {order.deliveryStaffName}</p>}</section>
          <section className="admin-order-detail-card"><h4>Thanh toán</h4><p>{order.payment?.method === "vnpay" ? "VNPAY" : order.payment?.method === "cod" ? "COD" : "Chưa có phương thức"} · {order.payment ? paymentStatus[order.payment.status] ?? order.payment.status : "Chưa có giao dịch"}</p>{order.payment?.paidAt && <p>Đã thanh toán: {formatDateTime(order.payment.paidAt)}</p>}</section>
        </div>
      </div>
      {order.statusHistory.length > 0 && <section className="admin-order-detail-card admin-order-detail-history"><h4>Lịch sử trạng thái</h4><ol>{order.statusHistory.map((entry) => <li key={entry.id}><strong>{orderStatusLabel[entry.status] ?? entry.status}</strong><span>{formatDateTime(entry.changedAt)}</span>{entry.note && <p>{entry.note}</p>}</li>)}</ol></section>}
    </div>
  );
}
