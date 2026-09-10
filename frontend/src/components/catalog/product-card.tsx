"use client";

import { PackageOpen } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { CatalogImage } from "@/components/catalog/catalog-image";
import { AddToCart, FavoriteButton } from "@/components/commerce/product-actions";
import { formatPrice, primaryProductImage, productStatusLabel } from "@/lib/catalog-format";
import type { Product } from "@/types/catalog";

export function ProductCard({ product }: { product: Product }) {
  const router = useRouter();
  const openDetail = () => router.push(`/products/${product.slug}`);

  return (
    <article className="product-card product-card-clickable"
      onClick={(event) => {
        if ((event.target as HTMLElement).closest("a, button, input")) return;
        openDetail();
      }}>
      <div className="product-card-media">
      <Link href={`/products/${product.slug}`} className="product-card-image" aria-label={`Xem ${product.name}`}>
        <CatalogImage src={primaryProductImage(product)} alt={product.name} />
        <span className={`stock-badge ${product.status}`}>
          {productStatusLabel[product.status]}
        </span>
      </Link>
      <FavoriteButton product={product} />
      </div>
      <div className="product-card-body">
        <Link className="category-kicker" href={`/categories/${product.category.slug}`}>
          {product.category.name}
        </Link>
        <h3><Link href={`/products/${product.slug}`}>{product.name}</Link></h3>
        <p className="product-description">{product.description ?? "Nông sản được chọn lọc cho bữa ăn hằng ngày."}</p>
        <div className="product-meta-row">
          <span>{product.category.name}</span>
          <span>{product.stock} còn lại</span>
        </div>
        <div className="product-card-footer">
          <div className="product-price">
            <strong>{formatPrice(product.price)}</strong>
            <span>/{product.unit ?? "sản phẩm"}</span>
          </div>
          <AddToCart product={product} compact />
        </div>
        {product.status === "out_of_stock" && (
          <span className="stock-note"><PackageOpen size={15} /> Tạm hết hàng</span>
        )}
      </div>
    </article>
  );
}
