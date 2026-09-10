"use client";

import { Heart, ShoppingBasket } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import { useCommerce } from "@/components/commerce/commerce-provider";
import { useAuthStore } from "@/stores/auth-store";
import type { Product } from "@/types/catalog";

export function FavoriteButton({ product }: { product: Product }) {
  const commerce = useCommerce();
  const router = useRouter();
  const hasHydrated = useAuthStore((state) => state.hasHydrated);
  const saved = commerce.wishlist.items.some((item) => item.product.id === product.id);

  return (
    <button type="button" className={`product-favorite${saved ? " is-saved" : ""}`}
      aria-label={`${saved ? "Bỏ yêu thích" : "Yêu thích"} ${product.name}`} aria-pressed={saved}
      disabled={!hasHydrated || commerce.busy}
      onClick={() => {
        if (!commerce.enabled) { router.push("/login"); return; }
        void commerce.toggleWishlist(product.id);
      }}>
      <Heart size={18} fill={saved ? "currentColor" : "none"} aria-hidden="true" />
    </button>
  );
}

export function AddToCart({ product, compact = false }: { product: Product; compact?: boolean }) {
  const [quantity, setQuantity] = useState("1");
  const commerce = useCommerce();
  const router = useRouter();
  const hasHydrated = useAuthStore((state) => state.hasHydrated);
  const inCart = commerce.cart.items.find((item) => item.product.id === product.id)?.quantity ?? 0;
  const remaining = Math.max(0, product.stock - inCart);
  const available = product.status === "in_stock" && remaining > 0;
  const disabled = !hasHydrated || !available || commerce.busy;

  const add = async () => {
    if (!commerce.enabled) { router.push("/login"); return; }
    const count = compact ? 1 : Number(quantity);
    if (!Number.isInteger(count) || count < 1 || count > remaining) {
      toast.error(`Vui lòng chọn từ 1 đến ${remaining} sản phẩm.`);
      return;
    }
    await commerce.addItem(product.id, count);
  };

  return (
    <div className={compact ? "compact-purchase" : "purchase-actions"}>
      {!compact && <label className="purchase-quantity">Số lượng
        <input type="number" min="1" max={remaining || 1} step="1" value={quantity}
          onChange={(event) => setQuantity(event.target.value)} disabled={disabled} />
      </label>}
      <button type="button" className={compact ? "product-add-button" : "primary-button"}
        disabled={disabled} onClick={() => void add()} aria-label={`Thêm ${product.name} vào giỏ hàng`}>
        <ShoppingBasket size={compact ? 15 : 19} aria-hidden="true" />
        <span>{available ? (compact ? "Thêm" : "Thêm vào giỏ hàng") : (remaining === 0 && inCart > 0 ? "Đã đủ số lượng" : "Hết hàng")}</span>
      </button>
      {!compact && <FavoriteButton product={product} />}
    </div>
  );
}
