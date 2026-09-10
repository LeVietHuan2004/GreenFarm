"use client";

import { ArrowLeft, Heart, Minus, Plus, ShoppingBasket, Trash2 } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { CatalogImage } from "@/components/catalog/catalog-image";
import { ProductCard } from "@/components/catalog/product-card";
import { useCommerce } from "@/components/commerce/commerce-provider";
import { formatPrice, primaryProductImage } from "@/lib/catalog-format";
import { useAuthStore } from "@/stores/auth-store";
import type { CartItem } from "@/types/commerce";

function CartRow({ item }: { item: CartItem }) {
  const { busy, loading, updateItem, removeItem } = useCommerce();
  const [quantity, setQuantity] = useState(String(item.quantity));
  const product = item.product;
  const available = product.status === "in_stock" && product.stock > 0;
  const overStock = item.quantity > product.stock;
  const parsed = Number(quantity);
  const valid = Number.isInteger(parsed) && parsed > 0 && parsed <= product.stock;
  const disabled = busy || loading;

  return (
    <article className="cart-row">
      <Link className="cart-image" href={`/products/${product.slug}`}><CatalogImage src={primaryProductImage(product)} alt={product.name} /></Link>
      <div className="cart-product-copy">
        <Link href={`/products/${product.slug}`}><h2>{product.name}</h2></Link>
        <p>{formatPrice(product.price)} / {product.unit ?? "sản phẩm"}</p>
        <small>Còn {product.stock} {product.unit ?? "sản phẩm"}</small>
        {(!available || overStock) && <p className="field-error" role="status">{!available ? "Sản phẩm hiện không còn bán. Vui lòng xóa khỏi giỏ." : "Số lượng trong giỏ vượt tồn kho. Vui lòng giảm số lượng."}</p>}
      </div>
      <form className="cart-quantity-form" onSubmit={async (event) => {
        event.preventDefault();
        if (valid && available) await updateItem(item.id, parsed);
      }}>
        <div className="quantity-stepper">
          <button type="button" aria-label={`Giảm số lượng ${product.name}`} disabled={disabled || !available || item.quantity <= 1}
            onClick={() => void updateItem(item.id, Math.min(item.quantity - 1, product.stock))}><Minus size={15} /></button>
          <input aria-label={`Số lượng ${product.name}`} type="number" min="1" max={product.stock || 1} step="1"
            value={quantity} onChange={(event) => setQuantity(event.target.value)} disabled={disabled || !available} aria-invalid={!valid} />
          <button type="button" aria-label={`Tăng số lượng ${product.name}`} disabled={disabled || !available || item.quantity >= product.stock}
            onClick={() => void updateItem(item.id, item.quantity + 1)}><Plus size={15} /></button>
        </div>
        {quantity !== String(item.quantity) && <button type="submit" className="cart-update" disabled={disabled || !available || !valid}>Cập nhật</button>}
      </form>
      <strong className="cart-line-total">{formatPrice(item.lineTotal)}</strong>
      <button type="button" className="cart-remove" aria-label={`Xóa ${product.name} khỏi giỏ hàng`} disabled={disabled} onClick={() => void removeItem(item.id)}><Trash2 size={18} /></button>
    </article>
  );
}

export function CommercePage({ kind }: { kind: "cart" | "wishlist" }) {
  const commerce = useCommerce();
  const { hasHydrated, user } = useAuthStore();
  const isCart = kind === "cart";
  const title = isCart ? "Giỏ hàng của bạn" : "Sản phẩm yêu thích";
  const Icon = isCart ? ShoppingBasket : Heart;
  const count = isCart ? commerce.cart.totalItems : commerce.wishlist.count;

  return (
    <main className="catalog-main commerce-main">
      <Link className="commerce-back" href="/products"><ArrowLeft size={16} /> Tiếp tục mua sắm</Link>
      <header className="commerce-heading"><span className="eyebrow">Cửa hàng GreenFarm</span><h1><Icon size={30} />{title}</h1>
        {commerce.enabled && <p>{count} sản phẩm · Được lưu trong tài khoản của bạn</p>}
      </header>
      {!hasHydrated || commerce.loading ? <p className="commerce-state" role="status">Đang tải {isCart ? "giỏ hàng" : "danh sách yêu thích"}...</p>
        : !commerce.enabled ? <div className="catalog-empty"><Icon size={42} /><h2>{user ? "Dành cho tài khoản khách hàng" : "Đăng nhập để lưu sản phẩm"}</h2><p>Giỏ hàng và danh sách yêu thích sẽ được lưu để bạn tiếp tục mua sắm lần sau.</p><Link href="/login" className="primary-button">Đăng nhập khách hàng</Link></div>
        : commerce.error ? <div className="catalog-empty" role="alert"><h2>Chưa thể tải dữ liệu</h2><p>{commerce.error}</p><button type="button" className="primary-button" onClick={() => void commerce.refresh()}>Thử lại</button></div>
        : count === 0 ? <div className="catalog-empty"><Icon size={44} /><h2>{isCart ? "Giỏ hàng đang trống" : "Chưa có sản phẩm yêu thích"}</h2><p>{isCart ? "Chọn nông sản tươi cho bữa ăn hôm nay." : "Nhấn trái tim trên sản phẩm để lưu vào danh sách này."}</p><Link href="/products" className="primary-button">Khám phá sản phẩm</Link></div>
        : isCart ? <div className="cart-layout">
          <section className="cart-items" aria-label="Sản phẩm trong giỏ hàng">
            {commerce.cart.items.map((item) => <CartRow key={`${item.id}:${item.quantity}:${item.product.stock}`} item={item} />)}
          </section>
          <aside className="cart-summary"><h2>Tóm tắt giỏ hàng</h2><div><span>Số lượng</span><strong>{commerce.cart.totalItems}</strong></div><div className="cart-subtotal"><span>Tạm tính</span><strong>{formatPrice(commerce.cart.subtotal)}</strong></div><p>Phí giao hàng và ưu đãi được tính ở bước tiếp theo.</p><Link href="/checkout" className="primary-button">Tiến hành thanh toán</Link><Link href="/products" className="secondary-button">Tiếp tục mua sắm</Link><button type="button" className="secondary-button" disabled={commerce.busy} onClick={() => void commerce.clearCart()}><Trash2 size={16} /> Xóa toàn bộ giỏ hàng</button></aside>
        </div>
        : <section className="product-grid" aria-label="Sản phẩm yêu thích">{commerce.wishlist.items.map((item) => <ProductCard key={item.id} product={item.product} />)}</section>}
    </main>
  );
}
