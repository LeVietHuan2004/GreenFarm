"use client";

import { BadgePercent, Check, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

import { formatPrice } from "@/lib/catalog-format";
import { rankVouchers } from "@/lib/voucher-ranking";
import type { AvailableCoupon } from "@/types/coupon";
import type { Cart } from "@/types/commerce";

export type VoucherSelection = {
  code: string;
  type: AvailableCoupon["couponType"];
};

type Props = {
  coupons: AvailableCoupon[];
  cart: Cart;
  shippingFee: number;
  loading: boolean;
  error: boolean;
  selected: VoucherSelection | null;
  onConfirm: (voucher: VoucherSelection | null) => Promise<boolean> | boolean;
};

const groups = [
  { type: "ORDER_DISCOUNT", title: "Mã giảm trên đơn" },
  { type: "FREESHIP", title: "Mã miễn phí vận chuyển" }
] as const;

function offer(coupon: AvailableCoupon) {
  if (coupon.couponType === "FREESHIP") {
    return coupon.maxDiscountAmount
      ? `Giảm phí vận chuyển tối đa ${formatPrice(coupon.maxDiscountAmount)}`
      : "Miễn phí vận chuyển";
  }
  const discount = coupon.discountType === "PERCENTAGE"
    ? `Giảm ${coupon.discountPercentage}%`
    : `Giảm ${formatPrice(coupon.discountAmount ?? 0)}`;
  return coupon.maxDiscountAmount && coupon.discountType === "PERCENTAGE"
    ? `${discount}, tối đa ${formatPrice(coupon.maxDiscountAmount)}`
    : discount;
}

export function VoucherPicker({ coupons, cart, shippingFee, loading, error, selected, onConfirm }: Props) {
  const [open, setOpen] = useState(false);
  const [draftCode, setDraftCode] = useState("");
  const [draftType, setDraftType] = useState<VoucherSelection["type"] | null>(null);
  const [filterType, setFilterType] = useState<"ALL" | VoucherSelection["type"]>("ALL");
  const [saving, setSaving] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const dialogRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const trigger = triggerRef.current;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    inputRef.current?.focus();
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
      if (event.key === "Tab") {
        const focusable = dialogRef.current?.querySelectorAll<HTMLElement>("button:not([disabled]), input:not([disabled])");
        if (!focusable?.length) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
      }
    };
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
      trigger?.focus();
    };
  }, [open]);

  const startChoosing = () => {
    setDraftCode(selected?.code ?? "");
    setDraftType(selected?.type ?? null);
    setFilterType("ALL");
    setOpen(true);
  };

  const changeCode = (code: string) => {
    const normalized = code.toUpperCase();
    const known = coupons.find(coupon => coupon.code.toUpperCase() === normalized.trim());
    setDraftCode(normalized);
    setDraftType(known?.couponType ?? null);
  };

  const confirm = async (voucher: VoucherSelection | null) => {
    setSaving(true);
    try {
      if (await onConfirm(voucher)) setOpen(false);
    } finally {
      setSaving(false);
    }
  };

  const code = draftCode.trim();
  const search = code.toLocaleLowerCase("vi-VN");
  const ranked = useMemo(() => rankVouchers(coupons, cart, shippingFee), [coupons, cart, shippingFee]);
  const matches = ranked.filter(({ coupon }) =>
    (filterType === "ALL" || coupon.couponType === filterType)
    && (!search || `${coupon.code} ${coupon.name} ${coupon.description ?? ""}`.toLocaleLowerCase("vi-VN").includes(search))
  );
  const knownCoupon = coupons.find(coupon => coupon.code.toUpperCase() === code);
  const type = knownCoupon?.couponType ?? draftType;
  const knownBenefit = ranked.find(item => item.coupon.code.toUpperCase() === code);
  const canConfirm = Boolean(code && type && (!knownBenefit || knownBenefit.available));

  return (
    <section className="voucher-picker">
      <div className="voucher-picker-label">
        <BadgePercent size={22} aria-hidden="true" />
        <span><strong>Mã giảm giá</strong><small>{selected ? `${selected.code} · ${selected.type === "FREESHIP" ? "Miễn phí vận chuyển" : "Giảm trên đơn"}` : "Chọn một mã giảm giá cho đơn hàng"}</small></span>
      </div>
      <button ref={triggerRef} className="voucher-picker-trigger" type="button" onClick={startChoosing}>{selected ? "Đổi mã" : "Chọn mã"}</button>

      {open && <div className="voucher-modal">
        <button className="voucher-modal-backdrop" type="button" aria-label="Đóng danh sách mã giảm giá" onClick={() => setOpen(false)} />
        <div ref={dialogRef} className="voucher-modal-dialog" role="dialog" aria-modal="true" aria-labelledby="voucher-modal-title">
          <header className="voucher-modal-header">
            <h2 id="voucher-modal-title">Chọn mã giảm giá</h2>
            <button type="button" aria-label="Đóng" onClick={() => setOpen(false)}><X size={19} /></button>
          </header>
          <div className="voucher-modal-content">
            <label className="voucher-code-label" htmlFor="voucher-code">Nhập mã hoặc chọn trong danh sách</label>
            <input id="voucher-code" ref={inputRef} value={draftCode} onChange={event => changeCode(event.target.value)} onKeyDown={event => { if (event.key === "Enter" && code && type && canConfirm && !saving) { event.preventDefault(); void confirm({ code, type }); } }} placeholder="Nhập mã giảm giá" maxLength={255} autoComplete="off" />
            {code && !knownCoupon && <fieldset className="voucher-manual-type">
              <legend>Loại mã nhập thủ công</legend>
              {groups.map(group => <label key={group.type}><input type="radio" name="manualVoucherType" checked={draftType === group.type} onChange={() => setDraftType(group.type)} />{group.title}</label>)}
            </fieldset>}
            <div className="voucher-filter-tabs" aria-label="Lọc loại mã">
              <button type="button" aria-pressed={filterType === "ALL"} onClick={() => setFilterType("ALL")}>Tất cả</button>
              {groups.map(group => <button type="button" key={group.type} aria-pressed={filterType === group.type} onClick={() => setFilterType(group.type)}>{group.title}</button>)}
            </div>
            {loading ? <p className="voucher-list-message">Đang tải mã giảm giá...</p> : error ? <p className="voucher-list-message">Không tải được danh sách. Bạn vẫn có thể nhập mã thủ công.</p> : <div className="voucher-ranked-list">
              {matches.length === 0 ? <p className="voucher-list-message">Không có mã phù hợp.</p> : matches.map(({ coupon, savings, available, reason }, index) => {
                const active = code === coupon.code.toUpperCase() && type === coupon.couponType;
                return <button className={`voucher-option${active ? " selected" : ""}`} key={coupon.code} type="button" aria-pressed={active} disabled={!available} onClick={() => { setDraftCode(coupon.code); setDraftType(coupon.couponType); }}>
                  <span className="voucher-option-mark">{active && <Check size={14} />}</span>
                  <span className="voucher-option-copy">
                    <span className="voucher-option-title"><strong>{coupon.code}</strong>{filterType === "ALL" && !search && index === 0 && savings != null && savings > 0 && <em>Lợi nhất</em>}</span>
                    <small className="voucher-type-label">{coupon.couponType === "FREESHIP" ? "Miễn phí vận chuyển" : "Giảm trên đơn"}</small>
                    <span>{offer(coupon)}</span>
                    <small>{coupon.name}{coupon.minimumOrderAmount ? ` · Đơn từ ${formatPrice(coupon.minimumOrderAmount)}` : ""}</small>
                    <strong className="voucher-estimated-saving">{savings != null && savings > 0 ? `Ước tính tiết kiệm ${formatPrice(savings)}` : reason ?? "Cần kiểm tra khi áp dụng"}</strong>
                  </span>
                </button>;
              })}
            </div>}
            <small className="voucher-condition-note">Số tiền chỉ là ước tính; hệ thống sẽ kiểm tra lại điều kiện và mức giảm khi tính đơn hàng.</small>
          </div>
          <footer className="voucher-modal-footer">
            {selected && <button className="voucher-clear" type="button" disabled={saving} onClick={() => void confirm(null)}>Bỏ mã</button>}
            <button className="voucher-cancel" type="button" disabled={saving} onClick={() => setOpen(false)}>Hủy</button>
            <button className="voucher-confirm" type="button" disabled={saving || !canConfirm} onClick={() => { if (code && type && canConfirm) void confirm({ code, type }); }}>{saving ? "Đang xử lý..." : "Xác nhận"}</button>
          </footer>
        </div>
      </div>}
    </section>
  );
}
