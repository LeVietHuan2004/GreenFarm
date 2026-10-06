export type AvailableCoupon = {
  code: string;
  name: string;
  description: string | null;
  couponType: "ORDER_DISCOUNT" | "FREESHIP";
  discountType: "PERCENTAGE" | "FIXED_AMOUNT";
  discountPercentage: number;
  discountAmount: number | null;
  maxDiscountAmount: number | null;
  minimumOrderAmount: number | null;
  scopeType: "ALL" | "CATEGORY" | "PRODUCT";
  expiresAt: string | null;
};
