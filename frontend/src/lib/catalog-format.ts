import type { Product, ProductStatus } from "@/types/catalog";

export const formatPrice = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0
}).format;

export const productStatusLabel: Record<ProductStatus, string> = {
  in_stock: "Còn hàng",
  out_of_stock: "Hết hàng",
  hidden: "Đang ẩn"
};

export function primaryProductImage(product: Product): string | null {
  return product.images.find(({ image }) => /^https?:\/\//i.test(image))?.image
    ?? product.images[0]?.image
    ?? null;
}
