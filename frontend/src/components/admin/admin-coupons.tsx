"use client";

import { BadgePercent, CalendarClock, Pencil, Plus, RotateCcw, TicketPercent } from "lucide-react";
import { FormEvent, useCallback, useEffect, useState } from "react";

import { AdminShell } from "@/components/admin/admin-shell";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { createAdminCoupon, getAdminCoupons, updateAdminCoupon, updateAdminCouponActive } from "@/services/admin-service";
import { useAuthStore } from "@/stores/auth-store";
import type { AdminCoupon, AdminCouponInput, CouponType, DiscountType } from "@/types/admin";
import type { PageData } from "@/types/catalog";

type CouponForm = {
  code: string; couponType: CouponType; discountType: DiscountType; discountPercentage: string;
  discountAmount: string; startsAt: string; expiresAt: string; usageLimit: string; active: boolean;
};

const emptyPage: PageData<AdminCoupon> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true };
const emptyForm: CouponForm = { code: "", couponType: "ORDER_DISCOUNT", discountType: "PERCENTAGE", discountPercentage: "10", discountAmount: "", startsAt: "", expiresAt: "", usageLimit: "", active: true };

function toForm(coupon: AdminCoupon): CouponForm {
  return {
    code: coupon.code, couponType: coupon.couponType, discountType: coupon.discountType,
    discountPercentage: String(coupon.discountPercentage), discountAmount: coupon.discountAmount == null ? "" : String(coupon.discountAmount),
    startsAt: coupon.startsAt?.slice(0, 16) ?? "", expiresAt: coupon.expiresAt?.slice(0, 16) ?? "",
    usageLimit: coupon.usageLimit == null ? "" : String(coupon.usageLimit), active: coupon.active
  };
}

function toInput(form: CouponForm): AdminCouponInput {
  return {
    code: form.code.trim().toUpperCase(), couponType: form.couponType, discountType: form.discountType,
    discountPercentage: form.couponType === "FREESHIP" ? 0 : Number(form.discountPercentage || 0),
    discountAmount: form.couponType === "ORDER_DISCOUNT" && form.discountType === "FIXED_AMOUNT" ? Number(form.discountAmount || 0) : null,
    startsAt: form.startsAt || null, expiresAt: form.expiresAt || null,
    usageLimit: form.usageLimit ? Number(form.usageLimit) : null, active: form.active
  };
}

export function AdminCoupons() {
  const { user, token, hasHydrated } = useAuthStore();
  const adminReady = hasHydrated && Boolean(token) && user?.role === "admin";
  const [coupons, setCoupons] = useState<PageData<AdminCoupon>>(emptyPage);
  const [activeFilter, setActiveFilter] = useState<"" | "true" | "false">("");
  const [form, setForm] = useState<CouponForm>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadCoupons = useCallback(async () => {
    try {
      setCoupons(await getAdminCoupons({ active: activeFilter === "" ? undefined : activeFilter === "true", page: 0, size: 50, sort: "createdAt,desc" }));
      setError(null);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    }
  }, [activeFilter]);

  useEffect(() => {
    if (!adminReady) return;
    const timer = window.setTimeout(() => { void loadCoupons(); }, 0);
    return () => window.clearTimeout(timer);
  }, [adminReady, loadCoupons]);

  const resetForm = () => { setEditingId(null); setForm(emptyForm); };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true); setError(null); setMessage(null);
    try {
      const saved = editingId == null ? await createAdminCoupon(toInput(form)) : await updateAdminCoupon(editingId, toInput(form));
      setMessage(`Đã ${editingId == null ? "tạo" : "cập nhật"} mã ${saved.code}.`);
      resetForm();
      await loadCoupons();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  };

  const toggleActive = async (coupon: AdminCoupon) => {
    setError(null); setMessage(null);
    try {
      const updated = await updateAdminCouponActive(coupon.id, !coupon.active);
      setCoupons((current) => ({ ...current, content: current.content.map((item) => item.id === updated.id ? updated : item) }));
      setMessage(`${updated.code} đã được ${updated.active ? "kích hoạt" : "tạm dừng"}.`);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    }
  };

  return (
    <AdminShell active="coupons">
      <main className="admin-catalog-main admin-operations-main">
        <header className="admin-page-heading">
          <div><span className="eyebrow">Khuyến mãi · Coupon</span><h1>Quản lý mã giảm giá</h1><p>Tạo ưu đãi, đặt thời hạn và kiểm soát số lượt sử dụng.</p></div>
          <span className="admin-count"><TicketPercent size={15} /> {coupons.totalElements} mã</span>
        </header>
        {error && <p className="catalog-notice error">{error}</p>}
        {message && <p className="catalog-notice success">{message}</p>}

        <div className="admin-coupon-layout">
          <form className="admin-list-card admin-coupon-form" onSubmit={submit}>
            <div className="admin-form-heading"><span><Plus size={18} /></span><div><strong>{editingId == null ? "Tạo mã mới" : `Sửa ${form.code}`}</strong><small>Thông tin áp dụng tại checkout</small></div></div>
            <label>Mã coupon<input required maxLength={50} pattern="[A-Za-z0-9_-]+" value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value.toUpperCase() })} placeholder="GREEN10" /></label>
            <label>Loại ưu đãi<select value={form.couponType} onChange={(event) => setForm({ ...form, couponType: event.target.value as CouponType })}><option value="ORDER_DISCOUNT">Giảm giá đơn hàng</option><option value="FREESHIP">Miễn phí giao hàng</option></select></label>
            {form.couponType === "ORDER_DISCOUNT" && <>
              <label>Cách giảm<select value={form.discountType} onChange={(event) => setForm({ ...form, discountType: event.target.value as DiscountType })}><option value="PERCENTAGE">Theo phần trăm</option><option value="FIXED_AMOUNT">Số tiền cố định</option></select></label>
              {form.discountType === "PERCENTAGE" ? <label>Phần trăm (%)<input required type="number" min="1" max="100" value={form.discountPercentage} onChange={(event) => setForm({ ...form, discountPercentage: event.target.value })} /></label> : <label>Số tiền giảm<input required type="number" min="1" value={form.discountAmount} onChange={(event) => setForm({ ...form, discountAmount: event.target.value })} /></label>}
            </>}
            <div className="admin-form-grid"><label>Bắt đầu<input type="datetime-local" value={form.startsAt} onChange={(event) => setForm({ ...form, startsAt: event.target.value })} /></label><label>Hết hạn<input type="datetime-local" value={form.expiresAt} onChange={(event) => setForm({ ...form, expiresAt: event.target.value })} /></label></div>
            <label>Giới hạn lượt dùng<input type="number" min="1" value={form.usageLimit} onChange={(event) => setForm({ ...form, usageLimit: event.target.value })} placeholder="Không giới hạn" /></label>
            <label className="admin-checkbox"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Kích hoạt ngay</label>
            <div className="admin-form-actions"><button className="primary-button" disabled={saving} type="submit">{saving ? "Đang lưu..." : editingId == null ? "Tạo coupon" : "Lưu thay đổi"}</button>{editingId != null && <button className="secondary-button" type="button" onClick={resetForm}><RotateCcw size={15} /> Hủy sửa</button>}</div>
          </form>

          <section className="admin-list-card admin-coupon-list">
            <div className="admin-operations-toolbar"><label>Hiển thị<select value={activeFilter} onChange={(event) => setActiveFilter(event.target.value as typeof activeFilter)}><option value="">Tất cả</option><option value="true">Đang bật</option><option value="false">Đã tắt</option></select></label></div>
            {coupons.content.map((coupon) => <article className="admin-coupon-row" key={coupon.id}>
              <div className="admin-coupon-code"><span><BadgePercent size={19} /></span><div><strong>{coupon.code}</strong><small>{coupon.couponType === "FREESHIP" ? "Miễn phí giao hàng" : coupon.discountType === "PERCENTAGE" ? `Giảm ${coupon.discountPercentage}%` : `Giảm ${formatPrice(coupon.discountAmount ?? 0)}`}</small></div></div>
              <div className="admin-coupon-meta"><span><CalendarClock size={14} />{coupon.expiresAt ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(new Date(coupon.expiresAt)) : "Không hết hạn"}</span><span>{coupon.timesUsed}/{coupon.usageLimit ?? "∞"} lượt</span></div>
              <span className={`admin-coupon-state ${coupon.currentlyUsable ? "usable" : "unavailable"}`}>{coupon.currentlyUsable ? "Có thể dùng" : coupon.active ? "Chưa/đã hết hạn" : "Đã tắt"}</span>
              <div className="admin-coupon-actions"><button type="button" onClick={() => { setEditingId(coupon.id); setForm(toForm(coupon)); }}><Pencil size={15} /> Sửa</button><button type="button" onClick={() => void toggleActive(coupon)}>{coupon.active ? "Tạm dừng" : "Kích hoạt"}</button></div>
            </article>)}
            {coupons.content.length === 0 && <div className="admin-empty">Chưa có mã giảm giá phù hợp.</div>}
          </section>
        </div>
      </main>
    </AdminShell>
  );
}
