"use client";

import { useState } from "react";
import { formatPrice } from "@/lib/catalog-format";
import type { AvailableCoupon } from "@/types/coupon";

type Props = {
  type: AvailableCoupon["couponType"];
  value: string;
  onChange: (code: string) => void;
  coupons: AvailableCoupon[];
  loading: boolean;
  error: boolean;
  placeholder: string;
};

function offer(coupon: AvailableCoupon) {
  if (coupon.couponType === "FREESHIP") {
    return coupon.maxDiscountAmount ? `Miễn phí vận chuyển tối đa ${formatPrice(coupon.maxDiscountAmount)}` : "Miễn phí vận chuyển";
  }
  const discount = coupon.discountType === "PERCENTAGE"
    ? `Giảm ${coupon.discountPercentage}%`
    : `Giảm ${formatPrice(coupon.discountAmount ?? 0)}`;
  return coupon.maxDiscountAmount && coupon.discountType === "PERCENTAGE"
    ? `${discount}, tối đa ${formatPrice(coupon.maxDiscountAmount)}`
    : discount;
}

export function CouponCodeInput({type, value, onChange, coupons, loading, error, placeholder}: Props) {
  const [open, setOpen] = useState(false);
  const [filtering, setFiltering] = useState(false);
  const heading = type === "FREESHIP" ? "Mã freeship" : "Mã giảm trên đơn";
  const search = filtering ? value.trim().toLocaleLowerCase("vi-VN") : "";
  const matches = coupons.filter(coupon => coupon.couponType === type &&
    (!search || `${coupon.code} ${coupon.name} ${coupon.description ?? ""}`.toLocaleLowerCase("vi-VN").includes(search)));

  return <div className="coupon-code-picker" onBlur={event => {
    if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
  }}>
    <input
      value={value}
      onChange={event => {onChange(event.target.value.toUpperCase());setFiltering(true);setOpen(true);}}
      onFocus={() => {setFiltering(false);setOpen(true);}}
      onKeyDown={event => {if (event.key === "Escape") setOpen(false);}}
      placeholder={placeholder}
      maxLength={255}
      autoComplete="off"
      aria-label={heading}
    />
    {open && <div className="coupon-suggestions">
      <strong className="coupon-suggestions-heading">{heading}</strong>
      {loading ? <p>Đang tải mã giảm giá...</p> : error ? <p>Chưa tải được danh sách mã. Bạn vẫn có thể nhập mã thủ công.</p> : matches.length === 0 ? <p>{search ? "Không tìm thấy mã phù hợp." : "Hiện chưa có mã nào."}</p> :
        <div className="coupon-suggestions-list">{matches.map(coupon => <button key={coupon.code} type="button" onClick={() => {onChange(coupon.code);setOpen(false);}}>
          <span className="coupon-suggestion-top"><strong>{coupon.code}</strong><span>{offer(coupon)}</span></span>
          <span>{coupon.name}</span>
          {coupon.description && <small>{coupon.description}</small>}
          {coupon.minimumOrderAmount != null && coupon.minimumOrderAmount > 0 && <small>Đơn tối thiểu {formatPrice(coupon.minimumOrderAmount)}</small>}
          {coupon.scopeType !== "ALL" && <small>Áp dụng cho {coupon.scopeType === "CATEGORY" ? "danh mục" : "sản phẩm"} được chọn</small>}
        </button>)}</div>}
      <small>Điều kiện mã sẽ được kiểm tra khi tính đơn hàng.</small>
    </div>}
  </div>;
}
