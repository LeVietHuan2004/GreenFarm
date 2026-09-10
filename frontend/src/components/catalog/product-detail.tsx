"use client";

import { ArrowLeft, CheckCircle2, PackageOpen, ShieldCheck, Sprout, Truck } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

import { CatalogImage } from "@/components/catalog/catalog-image";
import { AddToCart } from "@/components/commerce/product-actions";
import { formatPrice, primaryProductImage, productStatusLabel } from "@/lib/catalog-format";
import { getApiErrorMessage } from "@/lib/api-error";
import { getPublicProduct } from "@/services/catalog-service";
import type { Product } from "@/types/catalog";

export function ProductDetail({ slug }: { slug: string }) {
  const [product, setProduct] = useState<Product | null>(null);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getPublicProduct(slug)
      .then((data) => {
        if (!active) return;
        setProduct(data);
        setSelectedImage(primaryProductImage(data));
      })
      .catch((requestError) => active && setError(getApiErrorMessage(requestError)));
    return () => { active = false; };
  }, [slug]);

  if (error) {
    return <main className="catalog-main"><div className="catalog-empty"><PackageOpen size={34} /><h1>Không thể mở sản phẩm</h1><p>{error}</p><Link className="primary-button" href="/products">Về catalog</Link></div></main>;
  }

  if (!product) {
    return <main className="catalog-main"><div className="detail-loading" aria-label="Đang tải chi tiết sản phẩm" /></main>;
  }

  return (
    <main className="catalog-main product-detail-main">
      <nav className="breadcrumb" aria-label="Điều hướng">
        <Link href="/products"><ArrowLeft size={16} /> Sản phẩm</Link>
        <span>/</span>
        <Link href={`/categories/${product.category.slug}`}>{product.category.name}</Link>
        <span>/</span>
        <strong>{product.name}</strong>
      </nav>

      <article className="product-detail-grid">
        <section className="product-gallery">
          <div className="product-detail-image">
            <CatalogImage src={selectedImage} alt={product.name} />
          </div>
          {product.images.length > 1 && (
            <div className="thumbnail-row">
              {product.images.map((image) => (
                <button type="button" className={selectedImage === image.image ? "active" : ""} onClick={() => setSelectedImage(image.image)} key={image.id}>
                  <CatalogImage src={image.image} alt={`${product.name} - ảnh ${image.id}`} />
                </button>
              ))}
            </div>
          )}
        </section>

        <section className="product-detail-copy">
          <Link className="category-kicker" href={`/categories/${product.category.slug}`}>{product.category.name}</Link>
          <h1>{product.name}</h1>
          {product.nameEn && <p className="product-name-en">{product.nameEn}</p>}
          <div className="detail-price">{formatPrice(product.price)} <span>/{product.unit ?? "sản phẩm"}</span></div>
          <span className={`detail-stock ${product.status}`}>
            {product.status === "in_stock" ? <CheckCircle2 size={17} /> : <PackageOpen size={17} />}
            {productStatusLabel[product.status]} · {product.stock} {product.unit ?? "sản phẩm"}
          </span>
          <p className="detail-description">{product.description ?? "Sản phẩm tươi được GreenFarm chọn lọc kỹ cho bữa ăn hằng ngày."}</p>
          <div className="detail-facts">
            <div><Sprout size={20} /><span><strong>Chọn lọc</strong><small>Kiểm tra chất lượng mỗi ngày</small></span></div>
            <div><ShieldCheck size={20} /><span><strong>An tâm</strong><small>Nguồn gốc minh bạch</small></span></div>
            <div><Truck size={20} /><span><strong>Giao tươi</strong><small>Đóng gói cẩn thận</small></span></div>
          </div>
          <AddToCart product={product} />
        </section>
      </article>
    </main>
  );
}
