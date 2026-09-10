"use client";

import { ArrowRight, Grid3X3, Leaf, MessageCircle, RotateCcw, ShoppingBasket, Sprout, Truck } from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { CatalogImage } from "@/components/catalog/catalog-image";
import { ProductCard } from "@/components/catalog/product-card";
import { getApiErrorMessage } from "@/lib/api-error";
import { getPublicCategories, getPublicProducts } from "@/services/catalog-service";
import type { Category, Product } from "@/types/catalog";

export function CatalogLanding() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [totalProducts, setTotalProducts] = useState(0);
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [sort, setSort] = useState("newest");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    Promise.all([
      getPublicCategories(),
      getPublicProducts({ size: 8, sort: "createdAt,desc" })
    ])
      .then(([categoryData, productData]) => {
        if (!active) return;
        setCategories(categoryData);
        setProducts(productData.content);
        setTotalProducts(productData.totalElements);
      })
      .catch((requestError) => active && setError(getApiErrorMessage(requestError)))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, []);

  const visibleProducts = useMemo(() => {
    const minimum = minPrice ? Number(minPrice) : 0;
    const maximum = maxPrice ? Number(maxPrice) : Number.POSITIVE_INFINITY;
    return products
      .filter((product) => product.price >= minimum && product.price <= maximum)
      .sort((first, second) => {
        if (sort === "price-asc") return first.price - second.price;
        if (sort === "price-desc") return second.price - first.price;
        return new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime();
      });
  }, [maxPrice, minPrice, products, sort]);

  const resetFilters = () => {
    setMinPrice("");
    setMaxPrice("");
    setSort("newest");
  };

  return (
    <>
      <section className="catalog-hero">
        <div className="catalog-hero-inner">
          <div className="catalog-hero-copy">
            <div className="hero-pill-row">
              <span className="hero-pill">Nông sản sạch</span>
              <span className="hero-pill">Giao nhanh 2h</span>
              <span className="hero-pill">Giá bình ổn</span>
            </div>
            <span className="hero-eyebrow">Vận hành nhanh trong ngày</span>
            <h1>GreenFarm</h1>
            <p>Trang mua nông sản cho khách hàng với danh mục rõ ràng, giá minh bạch, tồn kho cập nhật và giỏ hàng sẵn sàng cho đơn giao trong ngày.</p>
          </div>
          <div className="hero-stat-grid" id="delivery">
            <div className="hero-stat-card">
              <span><ShoppingBasket size={16} /></span>
              <strong>{totalProducts || 42}+</strong>
              <small>Sản phẩm sẵn sàng</small>
              <p>Đang mở bán tại cửa hàng</p>
            </div>
            <div className="hero-stat-card">
              <span><Leaf size={16} /></span>
              <strong>{categories.length || 5}+</strong>
              <small>Nhóm nông sản</small>
              <p>Lọc nhanh theo nhu cầu</p>
            </div>
            <div className="hero-stat-card">
              <span><Truck size={16} /></span>
              <strong>2h</strong>
              <small>Giao nhanh nội thành</small>
              <p>Ước tính giao trong ngày</p>
            </div>
          </div>
        </div>
      </section>

      <main className="catalog-main">
        <section className="catalog-section" id="categories">
          <div className="catalog-section-heading">
            <div><span className="eyebrow">Đi chợ theo mùa</span><h2>Danh mục nông sản</h2></div>
            <Link href="/products" className="category-all-link"><Grid3X3 size={15} /> Tất cả</Link>
          </div>
          {loading ? (
            <div className="category-card-grid catalog-skeleton-grid" aria-label="Đang tải danh mục" />
          ) : (
            <div className="category-card-grid">
              <Link href="/products" className="category-card category-card-all">
                <span className="category-card-icon"><Leaf size={19} /></span>
                <div>
                  <h3>Tất cả</h3>
                  <span>Tất cả sản phẩm đang mở bán</span>
                </div>
                <ArrowRight size={16} aria-hidden="true" />
              </Link>
              {categories.map((category) => (
                <Link href={`/categories/${category.slug}`} className="category-card" key={category.id}>
                  <CatalogImage src={category.image} alt={category.name} />
                  <div>
                    <h3>{category.name}</h3>
                    <span>{category.description ?? `${category.productCount} sản phẩm đang mở bán`}</span>
                  </div>
                  <ArrowRight size={16} aria-hidden="true" />
                </Link>
              ))}
            </div>
          )}
        </section>

        <section className="catalog-section featured-products-section">
          <div className="catalog-section-heading home-products-heading">
            <div>
              <span className="eyebrow">Sản phẩm nổi bật</span>
              <h2>Hàng tươi đang mở bán</h2>
              <p>{loading ? "Đang cập nhật sản phẩm..." : `${visibleProducts.length} sản phẩm phù hợp`}</p>
            </div>
            <form className="home-product-filters" onSubmit={(event) => event.preventDefault()}>
              <input type="number" min="0" value={minPrice} onChange={(event) => setMinPrice(event.target.value)} placeholder="Giá từ" aria-label="Giá từ" />
              <input type="number" min="0" value={maxPrice} onChange={(event) => setMaxPrice(event.target.value)} placeholder="Giá đến" aria-label="Giá đến" />
              <select value={sort} onChange={(event) => setSort(event.target.value)} aria-label="Sắp xếp sản phẩm">
                <option value="newest">Mới nhất</option>
                <option value="price-asc">Giá thấp đến cao</option>
                <option value="price-desc">Giá cao đến thấp</option>
              </select>
              <button type="button" onClick={resetFilters}><RotateCcw size={14} /> Xóa lọc</button>
            </form>
          </div>
          {error && <p className="catalog-notice error">{error}</p>}
          {loading ? (
            <div className="product-grid catalog-skeleton-grid" aria-label="Đang tải sản phẩm" />
          ) : visibleProducts.length > 0 ? (
            <div className="product-grid">
              {visibleProducts.map((product) => <ProductCard product={product} key={product.id} />)}
            </div>
          ) : !error ? (
            <div className="catalog-empty"><Sprout size={30} /><h3>Không có sản phẩm trong khoảng giá này</h3><button type="button" className="filter-reset-button" onClick={resetFilters}>Xóa bộ lọc</button></div>
          ) : null}
          <div className="catalog-more-row">
            <Link href="/products" className="text-link">Xem toàn bộ cửa hàng <ArrowRight size={17} /></Link>
          </div>
        </section>
      </main>

      <button className="support-fab" id="support" type="button" aria-label="Liên hệ hỗ trợ" title="Liên hệ hỗ trợ">
        <MessageCircle size={22} />
      </button>
    </>
  );
}
