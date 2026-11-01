"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { RoleGuard } from "@/components/auth/role-guard";
import { SiteHeader } from "@/components/layout/site-header";
import { getApiErrorMessage } from "@/lib/api-error";
import { staffInventoryService } from "@/services/operations-service";
import { useAuthStore } from "@/stores/auth-store";
import type { InventoryDetail, InventoryPage, InventoryProduct } from "@/types/inventory";

export function StaffInventory() {
  const ready = useAuthStore(state => state.hasHydrated && Boolean(state.token) && state.user?.role === "staff");
  const [input, setInput] = useState(""); const [search, setSearch] = useState(""); const [page, setPage] = useState(0);
  const [items, setItems] = useState<InventoryPage<InventoryProduct> | null>(null);
  const [detail, setDetail] = useState<InventoryDetail | null>(null);
  const [batchId, setBatchId] = useState(""); const [type, setType] = useState("ADJUSTMENT");
  const [quantity, setQuantity] = useState(""); const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false); const [error, setError] = useState<string | null>(null); const [message, setMessage] = useState<string | null>(null);
  const load = useCallback(async () => { try { setItems(await staffInventoryService.list(search, page)); setError(null); } catch (cause) { setError(getApiErrorMessage(cause)); } }, [search,page]);
  useEffect(() => { if (!ready) return; const timer = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(timer); }, [ready,load]);
  const open = async (id:number) => { setError(null); try { setDetail(await staffInventoryService.detail(id)); setBatchId(""); } catch (cause) { setError(getApiErrorMessage(cause)); } };
  const adjust = async (event:FormEvent) => { event.preventDefault(); setError(null); setMessage(null); setBusy(true);
    try { await staffInventoryService.adjust({ batchId:Number(batchId), quantityChange:Number(quantity), type, reason:reason.trim() }); setMessage("Đã lưu điều chỉnh kho."); setQuantity(""); setReason(""); if (detail) await open(detail.product.productId); await load(); }
    catch (cause) { setError(getApiErrorMessage(cause)); } finally { setBusy(false); }
  };
  return <RoleGuard allowedRoles={["staff"]} loginHref="/staff/login"><div className="portal-shell"><SiteHeader/><main className="portal-main operations-main">
    <header className="portal-heading"><div><span className="eyebrow">Vận hành</span><h1>Kho hàng</h1><p>Xem tồn kho và ghi nhận điều chỉnh theo từng lô.</p></div><Link className="secondary-button" href="/staff">Về đơn hàng</Link></header>
    {error && <p className="catalog-notice error" role="alert">{error}</p>}{message && <p className="catalog-notice success" role="status">{message}</p>}
    <form className="catalog-searchbar" onSubmit={event => { event.preventDefault(); setPage(0); setSearch(input.trim()); }}><input value={input} onChange={event => setInput(event.target.value)} placeholder="Tìm sản phẩm" aria-label="Tìm sản phẩm trong kho"/><button type="submit">Tìm</button></form>
    <section className="operations-list">{items?.content.map(item => <article className="operation-card" key={item.productId}><div className="operation-card-main"><strong>{item.productName}</strong><span>Khả dụng: {item.availableQuantity} {item.unit}</span></div><p className="operation-note">Tổng tồn: {item.totalQuantity} · {item.hasExpiredBatch ? "Có lô hết hạn" : item.hasExpiringBatch ? "Có lô sắp hết hạn" : "Theo dõi theo lô"}</p><button type="button" className="secondary-button" onClick={() => void open(item.productId)}>Xem lô hàng</button></article>)}
      {items && !items.content.length && <div className="admin-empty">Không có sản phẩm phù hợp.</div>}
    </section>
    {items && items.totalPages > 1 && <nav className="pagination" aria-label="Phân trang kho"><button type="button" disabled={items.first} onClick={() => setPage(value => value-1)}>Trước</button><span>Trang {items.page+1} / {items.totalPages}</span><button type="button" disabled={items.last} onClick={() => setPage(value => value+1)}>Sau</button></nav>}
    {detail && <section className="admin-list-card"><h2>Lô hàng · {detail.product.productName}</h2><p>Chọn lô để ghi nhận điều chỉnh. Số âm là giảm tồn; số dương là tăng tồn.</p>
      <form className="form-stack" onSubmit={adjust}><label>Lô hàng<select required value={batchId} onChange={event => setBatchId(event.target.value)}><option value="">Chọn lô</option>{detail.batches.map(batch => <option key={batch.id} value={batch.id}>{batch.batchCode} · Khả dụng {batch.availableQuantity} · HSD {batch.expiryDate ?? "chưa có"}</option>)}</select></label>
        <label>Loại điều chỉnh<select value={type} onChange={event => setType(event.target.value)}><option value="ADJUSTMENT">Kiểm kê</option><option value="DAMAGED">Hư hỏng</option><option value="EXPIRED">Hết hạn</option><option value="RETURN">Trả hàng</option></select></label>
        <label>Số lượng thay đổi<input type="number" step="1" required value={quantity} onChange={event => setQuantity(event.target.value)}/></label>
        <label>Lý do<input required maxLength={1000} value={reason} onChange={event => setReason(event.target.value)}/></label>
        <button className="primary-button" disabled={busy} type="submit">{busy ? "Đang lưu..." : "Lưu điều chỉnh"}</button></form></section>}
  </main></div></RoleGuard>;
}
