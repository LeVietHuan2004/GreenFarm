"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/admin/admin-shell";
import { getApiErrorMessage } from "@/lib/api-error";
import { inventoryService } from "@/services/inventory-service";
import { useAuthStore } from "@/stores/auth-store";
import type { InventoryAdjustment, InventoryBatch, InventoryDetail, InventoryImport, InventoryPage, InventoryProduct, InventorySummary, InventoryTransaction, Supplier, SupplierInput } from "@/types/inventory";

type Tab = "overview" | "stock" | "import" | "batches" | "history" | "suppliers";
const tabs: { key: Tab; label: string }[] = [
  { key: "overview", label: "Tổng quan kho" }, { key: "stock", label: "Tồn kho" },
  { key: "import", label: "Nhập kho" }, { key: "batches", label: "Lô hàng" },
  { key: "history", label: "Lịch sử kho" }, { key: "suppliers", label: "Nhà cung cấp" }
];
const stockLabels = { IN_STOCK: "Còn hàng", LOW_STOCK: "Sắp hết", OUT_OF_STOCK: "Hết hàng" };
const expiryLabels = { NO_EXPIRY: "Không HSD (dữ liệu cũ)", VALID: "Còn hạn", EXPIRING: "Sắp hết hạn", EXPIRED: "Đã hết hạn" };
const movementLabels: Record<string, string> = { IMPORT: "Nhập kho", EXPORT: "Xuất kho", RESERVE: "Giữ hàng", RESERVATION_RELEASE: "Hủy giữ hàng", ADJUSTMENT: "Kiểm kê", DAMAGED: "Hư hỏng", EXPIRED: "Hết hạn", RETURN: "Trả hàng", ORDER_CANCEL_RESTORE: "Hoàn đơn" };
const emptySupplier: SupplierInput = { supplierCode: "", name: "", phone: null, email: null, address: null, status: "active", note: null };
const emptyImport = { productId: "", quantity: "", importPrice: "0", manufactureDate: "", expiryDate: "", supplierId: "", note: "" };
const emptyAdjust = { batchId: "", quantityChange: "", type: "ADJUSTMENT", reason: "" };
const dateTime = (value: string) => new Date(value).toLocaleString("vi-VN");
const quantity = (value: number, unit?: string | null) => `${value.toLocaleString("vi-VN")} ${unit ?? ""}`.trim();
const money = (value: number) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value);
async function loadAll<T>(fetchPage: (page: number) => Promise<InventoryPage<T>>): Promise<T[]> {
  const first = await fetchPage(0);
  if (first.totalPages <= 1) return first.content;
  const rest = await Promise.all(Array.from({ length: first.totalPages - 1 }, (_, index) => fetchPage(index + 1)));
  return [first, ...rest].flatMap((page) => page.content);
}

export function AdminInventory() {
  const ready = useAuthStore((state) => state.hasHydrated && Boolean(state.token) && state.user?.role === "admin");
  const [tab, setTab] = useState<Tab>("overview");
  const [summary, setSummary] = useState<InventorySummary | null>(null);
  const [stock, setStock] = useState<InventoryPage<InventoryProduct> | null>(null);
  const [batches, setBatches] = useState<InventoryPage<InventoryBatch> | null>(null);
  const [history, setHistory] = useState<InventoryPage<InventoryTransaction> | null>(null);
  const [suppliers, setSuppliers] = useState<InventoryPage<Supplier> | null>(null);
  const [allProducts, setAllProducts] = useState<InventoryProduct[]>([]);
  const [allSuppliers, setAllSuppliers] = useState<Supplier[]>([]);
  const [detail, setDetail] = useState<InventoryDetail | null>(null);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("");
  const [sort, setSort] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [importForm, setImportForm] = useState(emptyImport);
  const [adjustForm, setAdjustForm] = useState(emptyAdjust);
  const [supplierForm, setSupplierForm] = useState<SupplierInput>(emptySupplier);
  const [editingSupplier, setEditingSupplier] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      if (tab === "overview") {
        const [stats, low, expiring, expired] = await Promise.all([
          inventoryService.summary(),
          inventoryService.inventory({ status: "LOW_STOCK", size: 10 }),
          inventoryService.batches({ expiry: "EXPIRING", size: 10 }),
          inventoryService.batches({ expiry: "EXPIRED", size: 10 })
        ]);
        setSummary(stats); setStock(low); setBatches({ ...expiring, content: [...expiring.content, ...expired.content] });
      } else if (tab === "stock") {
        setStock(await inventoryService.inventory({ search, status: filter === "EXPIRING" || filter === "EXPIRED" ? undefined : filter, expiry: filter === "EXPIRING" || filter === "EXPIRED" ? filter : undefined, sort, page, size: 20 }));
      } else if (tab === "batches") {
        setBatches(await inventoryService.batches({ search, expiry: filter, sort, page, size: 20 }));
      } else if (tab === "history") {
        setHistory(await inventoryService.transactions({ search, type: filter, from, to, sort, page, size: 20 }));
      } else if (tab === "suppliers") {
        setSuppliers(await inventoryService.suppliers({ search, status: filter, sort, page, size: 20 }));
      } else {
        const [products, supplierList] = await Promise.all([
          loadAll((index) => inventoryService.inventory({ page: index, size: 50 })),
          loadAll((index) => inventoryService.suppliers({ status: "active", page: index, size: 50 }))
        ]);
        setAllProducts(products); setAllSuppliers(supplierList);
      }
      setError(null);
    } catch (caught) { setError(getApiErrorMessage(caught)); }
    finally { setLoading(false); }
  }, [tab, search, filter, sort, from, to, page]);

  useEffect(() => {
    if (!ready) return;
    const timer = window.setTimeout(() => { void load(); }, 0);
    return () => window.clearTimeout(timer);
  }, [ready, load]);

  const selectTab = (next: Tab) => { setTab(next); setSearch(""); setFilter(""); setSort(""); setFrom(""); setTo(""); setPage(0); setDetail(null); setError(null); setMessage(null); };
  const submitImport = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true); setError(null); setMessage(null);
    try {
      const input: InventoryImport = { productId: Number(importForm.productId), quantity: Number(importForm.quantity), importPrice: Number(importForm.importPrice), manufactureDate: importForm.manufactureDate || null, expiryDate: importForm.expiryDate, supplierId: importForm.supplierId ? Number(importForm.supplierId) : null, note: importForm.note || null };
      const result = await inventoryService.importBatch(input);
      setMessage(`Đã nhập ${quantity(result.quantity, result.unit)} vào lô ${result.batchCode}.`);
      setImportForm(emptyImport); await load();
    } catch (caught) { setError(getApiErrorMessage(caught)); }
    finally { setSaving(false); }
  };
  const submitAdjustment = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true); setError(null); setMessage(null);
    try {
      const input: InventoryAdjustment = { batchId: Number(adjustForm.batchId), quantityChange: Number(adjustForm.quantityChange), type: adjustForm.type, reason: adjustForm.reason.trim() };
      const result = await inventoryService.adjust(input);
      setMessage(`Đã điều chỉnh lô ${result.batchCode}. Tồn hiện tại: ${quantity(result.remainingQuantity, result.unit)}.`);
      setAdjustForm(emptyAdjust); setDetail(null); await load();
    } catch (caught) { setError(getApiErrorMessage(caught)); }
    finally { setSaving(false); }
  };
  const submitSupplier = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true); setError(null); setMessage(null);
    try {
      if (editingSupplier) await inventoryService.updateSupplier(editingSupplier, supplierForm);
      else await inventoryService.createSupplier(supplierForm);
      setMessage(editingSupplier ? "Đã cập nhật nhà cung cấp." : "Đã thêm nhà cung cấp.");
      setSupplierForm(emptySupplier); setEditingSupplier(null); await load();
    } catch (caught) { setError(getApiErrorMessage(caught)); }
    finally { setSaving(false); }
  };
  const deactivate = async (supplier: Supplier) => {
    if (!window.confirm(`Ngừng sử dụng nhà cung cấp ${supplier.name}?`)) return;
    try { await inventoryService.deactivateSupplier(supplier.id); setMessage("Đã ngừng sử dụng nhà cung cấp."); await load(); }
    catch (caught) { setError(getApiErrorMessage(caught)); }
  };
  const showDetail = async (productId: number) => {
    try { setDetail(await inventoryService.detail(productId)); setError(null); }
    catch (caught) { setError(getApiErrorMessage(caught)); }
  };
  const pageData = tab === "stock" ? stock : tab === "batches" ? batches : tab === "history" ? history : tab === "suppliers" ? suppliers : null;

  return <AdminShell active="inventory"><main className="inventory-admin admin-catalog-main">
    <header className="admin-page-heading"><div><span className="eyebrow">Vận hành · Quản trị</span><h1>Quản lý kho</h1><p>Theo dõi lô nông sản, hạn sử dụng và mọi biến động tồn kho.</p></div></header>
    <nav className="inventory-tabs" aria-label="Các mục quản lý kho">{tabs.map((item) => <button key={item.key} type="button" className={tab === item.key ? "selected" : ""} onClick={() => selectTab(item.key)}>{item.label}</button>)}</nav>
    {error && <p className="inventory-alert error" role="alert">{error}</p>}
    {message && <p className="inventory-alert success" role="status">{message}</p>}
    {loading && <p className="inventory-muted">Đang tải dữ liệu kho...</p>}

    {tab === "overview" && <>
      <div className="inventory-stats">{[
        ["Sản phẩm đang tồn", summary?.productsInStock], ["Sản phẩm sắp hết", summary?.lowStockProducts],
        ["Lô sắp hết hạn", summary?.expiringBatches], ["Lô đã hết hạn", summary?.expiredBatches]
      ].map(([label, value]) => <article key={String(label)}><span>{label}</span><strong>{value ?? "—"}</strong></article>)}</div>
      <div className="inventory-split"><section className="inventory-panel"><h2>Tồn kho sắp hết</h2>{stock?.content.length ? stock.content.map((item) => <button key={item.productId} className="inventory-list-row" onClick={() => { selectTab("stock"); void showDetail(item.productId); }}><span>{item.productName}</span><strong>{quantity(item.availableQuantity, item.unit)}</strong></button>) : <p className="inventory-muted">Không có sản phẩm sắp hết.</p>}</section>
      <section className="inventory-panel"><h2>Cảnh báo hạn sử dụng</h2>{batches?.content.length ? batches.content.map((item) => <div className="inventory-list-row" key={item.id}><span>{item.batchCode} · {item.productName}<small>HSD {item.expiryDate ?? "—"}</small></span><b className={`inventory-badge ${item.expiryStatus.toLowerCase()}`}>{expiryLabels[item.expiryStatus]}</b></div>) : <p className="inventory-muted">Không có lô cần cảnh báo.</p>}</section></div>
    </>}

    {tab === "stock" && <>
      <Filters search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} sort={sort} setSort={setSort} sortOptions={[["", "Tên A–Z"], ["productName,desc", "Tên Z–A"], ["availableQuantity,asc", "Khả dụng tăng"], ["availableQuantity,desc", "Khả dụng giảm"], ["totalQuantity,desc", "Tổng tồn giảm"]]} resetPage={() => setPage(0)} options={[["", "Mọi trạng thái"], ["IN_STOCK", "Còn hàng"], ["LOW_STOCK", "Sắp hết"], ["OUT_OF_STOCK", "Hết hàng"], ["EXPIRING", "Có lô sắp hết hạn"], ["EXPIRED", "Có lô hết hạn"]]} />
      <div className="inventory-table-wrap"><table className="inventory-table"><thead><tr><th>Mã SP</th><th>Sản phẩm</th><th>Danh mục</th><th>Tổng tồn</th><th>Khả dụng</th><th>Trạng thái</th><th>HSD</th><th></th></tr></thead><tbody>{stock?.content.map((item) => <tr key={item.productId}><td>{item.productCode}</td><td><strong>{item.productName}</strong></td><td>{item.categoryName}</td><td>{quantity(item.totalQuantity, item.unit)}</td><td>{quantity(item.availableQuantity, item.unit)}</td><td><b className={`inventory-badge ${item.stockStatus.toLowerCase()}`}>{stockLabels[item.stockStatus]}</b></td><td>{item.hasExpiredBatch && <b className="inventory-badge expired">Đã hết hạn</b>} {item.hasExpiringBatch && <b className="inventory-badge expiring">Sắp hết hạn</b>}</td><td><button type="button" onClick={() => void showDetail(item.productId)}>Xem lô</button></td></tr>)}</tbody></table></div>
      {!stock?.content.length && !loading && <p className="inventory-muted">Không tìm thấy sản phẩm.</p>}
      {detail && <section className="inventory-panel inventory-detail"><div className="inventory-panel-head"><h2>{detail.product.productName} · Các lô hàng</h2><button type="button" onClick={() => setDetail(null)}>Đóng</button></div><BatchTable rows={detail.batches} onAdjust={(batch) => { setAdjustForm({ ...emptyAdjust, batchId: String(batch.id) }); window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" }); }} /></section>}
      {detail && <AdjustmentForm form={adjustForm} setForm={setAdjustForm} submit={submitAdjustment} saving={saving} />}
    </>}

    {tab === "import" && <section className="inventory-panel"><h2>Nhập kho theo lô</h2><p className="inventory-muted">Mỗi lần nhập tạo một mã lô riêng. Hạn sử dụng là bắt buộc.</p><form className="inventory-form" onSubmit={submitImport}>
      <label>Sản phẩm<select required value={importForm.productId} onChange={(e) => setImportForm({ ...importForm, productId: e.target.value })}><option value="">Chọn sản phẩm</option>{allProducts.map((item) => <option key={item.productId} value={item.productId}>{item.productCode} · {item.productName} ({item.unit ?? "sản phẩm"})</option>)}</select></label>
      <label>Số lượng nhập<input required type="number" min="1" step="1" value={importForm.quantity} onChange={(e) => setImportForm({ ...importForm, quantity: e.target.value })} /></label>
      <label>Giá nhập (VND)<input required type="number" min="0" step="0.01" value={importForm.importPrice} onChange={(e) => setImportForm({ ...importForm, importPrice: e.target.value })} /></label>
      <label>Ngày sản xuất / thu hoạch<input type="date" value={importForm.manufactureDate} onChange={(e) => setImportForm({ ...importForm, manufactureDate: e.target.value })} /></label>
      <label>Hạn sử dụng<input required type="date" value={importForm.expiryDate} onChange={(e) => setImportForm({ ...importForm, expiryDate: e.target.value })} /></label>
      <label>Nhà cung cấp<select value={importForm.supplierId} onChange={(e) => setImportForm({ ...importForm, supplierId: e.target.value })}><option value="">Chưa xác định</option>{allSuppliers.map((item) => <option key={item.id} value={item.id}>{item.supplierCode} · {item.name}</option>)}</select></label>
      <label className="wide">Ghi chú<textarea value={importForm.note} onChange={(e) => setImportForm({ ...importForm, note: e.target.value })} /></label>
      <button className="inventory-primary" disabled={saving}>Nhập kho</button>
    </form></section>}

    {tab === "batches" && <>
      <Filters search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} sort={sort} setSort={setSort} sortOptions={[["", "Mới nhập trước"], ["expiryDate,asc", "HSD gần trước"], ["remainingQuantity,asc", "Tồn tăng"], ["remainingQuantity,desc", "Tồn giảm"]]} resetPage={() => setPage(0)} options={[["", "Mọi hạn dùng"], ["VALID", "Còn hạn"], ["EXPIRING", "Sắp hết hạn"], ["EXPIRED", "Đã hết hạn"]]} />
      <BatchTable rows={batches?.content ?? []} onAdjust={(batch) => setAdjustForm({ ...emptyAdjust, batchId: String(batch.id) })} />
      {!batches?.content.length && !loading && <p className="inventory-muted">Không tìm thấy lô hàng.</p>}
      <AdjustmentForm form={adjustForm} setForm={setAdjustForm} submit={submitAdjustment} saving={saving} />
    </>}

    {tab === "history" && <>
      <Filters search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} sort={sort} setSort={setSort} sortOptions={[["", "Mới nhất"], ["createdAt,asc", "Cũ nhất"]]} resetPage={() => setPage(0)} options={[["", "Mọi giao dịch"], ...Object.entries(movementLabels)]} />
      <div className="inventory-filters inventory-date-filters"><label>Từ ngày <input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} /></label><label>Đến ngày <input type="date" min={from || undefined} value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} /></label></div>
      <div className="inventory-table-wrap"><table className="inventory-table"><thead><tr><th>Thời gian</th><th>Loại</th><th>Sản phẩm / Lô</th><th>Thay đổi</th><th>Tồn trước → sau</th><th>Tham chiếu</th><th>Lý do / Người thực hiện</th></tr></thead><tbody>{history?.content.map((item) => <tr key={item.transactionId}><td>{dateTime(item.createdAt)}</td><td>{movementLabels[item.type] ?? item.type}</td><td><strong>{item.productName}</strong><br />{item.batchCode}</td><td className={item.quantity < 0 ? "inventory-negative" : "inventory-positive"}>{item.quantity > 0 ? "+" : ""}{item.quantity}</td><td>{item.quantityBefore} → {item.quantityAfter}<small>Giữ: {item.reservedBefore} → {item.reservedAfter}</small></td><td>{item.referenceId ? `Đơn #${item.referenceId}` : "—"}</td><td>{item.reason ?? "—"}<small>{item.createdByName ?? "Hệ thống"}</small></td></tr>)}</tbody></table></div>
      {!history?.content.length && !loading && <p className="inventory-muted">Chưa có giao dịch phù hợp.</p>}
    </>}

    {tab === "suppliers" && <>
      <section className="inventory-panel"><h2>{editingSupplier ? "Sửa nhà cung cấp" : "Thêm nhà cung cấp"}</h2><form className="inventory-form" onSubmit={submitSupplier}>
        <label>Mã nhà cung cấp<input required maxLength={40} value={supplierForm.supplierCode} onChange={(e) => setSupplierForm({ ...supplierForm, supplierCode: e.target.value })} /></label>
        <label>Tên<input required maxLength={150} value={supplierForm.name} onChange={(e) => setSupplierForm({ ...supplierForm, name: e.target.value })} /></label>
        <label>Điện thoại<input maxLength={30} value={supplierForm.phone ?? ""} onChange={(e) => setSupplierForm({ ...supplierForm, phone: e.target.value || null })} /></label>
        <label>Email<input type="email" value={supplierForm.email ?? ""} onChange={(e) => setSupplierForm({ ...supplierForm, email: e.target.value || null })} /></label>
        <label>Địa chỉ<input value={supplierForm.address ?? ""} onChange={(e) => setSupplierForm({ ...supplierForm, address: e.target.value || null })} /></label>
        <label>Trạng thái<select value={supplierForm.status} onChange={(e) => setSupplierForm({ ...supplierForm, status: e.target.value as SupplierInput["status"] })}><option value="active">Hoạt động</option><option value="inactive">Ngừng hoạt động</option></select></label>
        <label className="wide">Ghi chú<textarea value={supplierForm.note ?? ""} onChange={(e) => setSupplierForm({ ...supplierForm, note: e.target.value || null })} /></label>
        <button className="inventory-primary" disabled={saving}>{editingSupplier ? "Lưu thay đổi" : "Thêm nhà cung cấp"}</button>{editingSupplier && <button type="button" onClick={() => { setEditingSupplier(null); setSupplierForm(emptySupplier); }}>Hủy sửa</button>}
      </form></section>
      <Filters search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} sort={sort} setSort={setSort} sortOptions={[["", "Tên A–Z"], ["name,desc", "Tên Z–A"], ["createdAt,desc", "Mới tạo trước"]]} resetPage={() => setPage(0)} options={[["", "Mọi trạng thái"], ["active", "Hoạt động"], ["inactive", "Ngừng hoạt động"]]} />
      <div className="inventory-table-wrap"><table className="inventory-table"><thead><tr><th>Mã</th><th>Tên</th><th>Liên hệ</th><th>Địa chỉ</th><th>Trạng thái</th><th></th></tr></thead><tbody>{suppliers?.content.map((item) => <tr key={item.id}><td>{item.supplierCode}</td><td><strong>{item.name}</strong></td><td>{item.phone ?? "—"}<small>{item.email}</small></td><td>{item.address ?? "—"}</td><td><b className={`inventory-badge ${item.status}`}>{item.status === "active" ? "Hoạt động" : "Ngừng"}</b></td><td><button type="button" onClick={() => { setEditingSupplier(item.id); setSupplierForm({ supplierCode: item.supplierCode, name: item.name, phone: item.phone, email: item.email, address: item.address, status: item.status, note: item.note }); window.scrollTo({ top: 0, behavior: "smooth" }); }}>Sửa</button> {item.status === "active" && <button type="button" onClick={() => void deactivate(item)}>Ngừng</button>}</td></tr>)}</tbody></table></div>
      {!suppliers?.content.length && !loading && <p className="inventory-muted">Chưa có nhà cung cấp phù hợp.</p>}
    </>}
    {pageData && pageData.totalPages > 1 && <div className="inventory-pagination"><span>Trang {pageData.page + 1}/{pageData.totalPages} · {pageData.totalElements} mục</span><button type="button" disabled={pageData.first} onClick={() => setPage((value) => value - 1)}>Trước</button><button type="button" disabled={pageData.last} onClick={() => setPage((value) => value + 1)}>Sau</button></div>}
  </main></AdminShell>;
}

function Filters({ search, setSearch, filter, setFilter, sort, setSort, sortOptions, resetPage, options }: { search: string; setSearch: (value: string) => void; filter: string; setFilter: (value: string) => void; sort: string; setSort: (value: string) => void; sortOptions: string[][]; resetPage: () => void; options: string[][] }) {
  return <div className="inventory-filters"><input aria-label="Tìm kiếm kho" placeholder="Tìm sản phẩm, mã lô hoặc lý do..." value={search} onChange={(e) => { setSearch(e.target.value); resetPage(); }} /><select aria-label="Lọc trạng thái kho" value={filter} onChange={(e) => { setFilter(e.target.value); resetPage(); }}>{options.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select><select aria-label="Sắp xếp" value={sort} onChange={(e) => { setSort(e.target.value); resetPage(); }}>{sortOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></div>;
}

function BatchTable({ rows, onAdjust }: { rows: InventoryBatch[]; onAdjust: (batch: InventoryBatch) => void }) {
  return <div className="inventory-table-wrap"><table className="inventory-table"><thead><tr><th>Mã lô</th><th>Sản phẩm</th><th>Nhập</th><th>Còn / Khả dụng</th><th>HSD</th><th>Nhà cung cấp</th><th>Ngày nhập</th><th></th></tr></thead><tbody>{rows.map((item) => <tr key={item.id}><td><strong>{item.batchCode}</strong></td><td>{item.productName}</td><td>{quantity(item.quantity, item.unit)}<small>{money(item.importPrice)}</small></td><td>{quantity(item.remainingQuantity, item.unit)}<small>Khả dụng {quantity(item.availableQuantity, item.unit)} · Giữ {item.reservedQuantity}</small></td><td>{item.expiryDate ?? "—"}<small><b className={`inventory-badge ${item.expiryStatus.toLowerCase()}`}>{expiryLabels[item.expiryStatus]}</b></small></td><td>{item.supplierName ?? "—"}</td><td>{dateTime(item.importedAt)}</td><td><button type="button" onClick={() => onAdjust(item)}>Điều chỉnh</button></td></tr>)}</tbody></table></div>;
}

function AdjustmentForm({ form, setForm, submit, saving }: { form: typeof emptyAdjust; setForm: (value: typeof emptyAdjust) => void; submit: (event: FormEvent) => void; saving: boolean }) {
  return <section className="inventory-panel inventory-adjust"><h2>Điều chỉnh lô</h2><p className="inventory-muted">Nhập số âm để giảm tồn, số dương để tăng. Không thể giảm phần đã giữ cho đơn.</p><form className="inventory-form" onSubmit={submit}>
    <label>ID lô<input type="number" min="1" required value={form.batchId} onChange={(e) => setForm({ ...form, batchId: e.target.value })} /></label>
    <label>Loại<select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}><option value="ADJUSTMENT">Kiểm kê / khác</option><option value="DAMAGED">Hư hỏng</option><option value="EXPIRED">Hết hạn</option><option value="RETURN">Trả hàng</option></select></label>
    <label>Số lượng thay đổi<input type="number" step="1" required value={form.quantityChange} onChange={(e) => setForm({ ...form, quantityChange: e.target.value })} /></label>
    <label className="wide">Lý do<input required maxLength={1000} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></label>
    <button className="inventory-primary" disabled={saving}>Lưu điều chỉnh</button>
  </form></section>;
}
