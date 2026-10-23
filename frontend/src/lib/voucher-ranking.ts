import type { AvailableCoupon } from "@/types/coupon";
import type { Cart } from "@/types/commerce";

export type VoucherBenefit = {
  coupon: AvailableCoupon;
  savings: number | null;
  available: boolean;
  reason: string | null;
};

function cap(amount: number, maximum: number | null) {
  return Math.max(0, Math.min(amount, maximum ?? amount));
}

export function estimateVoucherBenefit(coupon: AvailableCoupon, cart: Cart, shippingFee: number): VoucherBenefit {
  const subtotal = cart.subtotal;
  if (coupon.minimumOrderAmount != null && subtotal < coupon.minimumOrderAmount) {
    return { coupon, savings: 0, available: false, reason: `Đơn cần đạt ${coupon.minimumOrderAmount.toLocaleString("vi-VN")} đ` };
  }

  const scopeIds = coupon.scopeType === "CATEGORY" ? coupon.categoryIds : coupon.scopeType === "PRODUCT" ? coupon.productIds : [];
  if (scopeIds == null) {
    return { coupon, savings: null, available: true, reason: "Cần kiểm tra phạm vi áp dụng" };
  }
  const eligibleSubtotal = cart.items.reduce((total, item) => {
    const applies = coupon.scopeType === "ALL"
      || (coupon.scopeType === "CATEGORY" && scopeIds.includes(item.product.category.id))
      || (coupon.scopeType === "PRODUCT" && scopeIds.includes(item.product.id));
    return applies ? total + item.product.price * item.quantity : total;
  }, 0);
  if (eligibleSubtotal <= 0) {
    return { coupon, savings: 0, available: false, reason: "Không có sản phẩm phù hợp" };
  }

  if (coupon.couponType === "FREESHIP") {
    if (shippingFee <= 0) return { coupon, savings: 0, available: false, reason: "Đơn đã miễn phí vận chuyển" };
    return { coupon, savings: cap(shippingFee, coupon.maxDiscountAmount), available: true, reason: null };
  }

  const rawDiscount = coupon.discountType === "PERCENTAGE"
    ? Math.round(eligibleSubtotal * coupon.discountPercentage) / 100
    : coupon.discountAmount ?? 0;
  const savings = cap(Math.min(rawDiscount, eligibleSubtotal), coupon.maxDiscountAmount);
  return { coupon, savings, available: savings > 0, reason: savings > 0 ? null : "Mã chưa tạo ưu đãi cho đơn này" };
}

export function rankVouchers(coupons: AvailableCoupon[], cart: Cart, shippingFee: number) {
  return coupons.map(coupon => estimateVoucherBenefit(coupon, cart, shippingFee)).sort((first, second) => {
    const priority = (item: VoucherBenefit) => item.available ? (item.savings == null ? 1 : 2) : 0;
    return priority(second) - priority(first)
      || (second.savings ?? 0) - (first.savings ?? 0)
      || first.coupon.code.localeCompare(second.coupon.code, "vi-VN");
  });
}
