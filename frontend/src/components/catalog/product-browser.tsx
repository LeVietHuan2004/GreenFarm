"use client";

import { ChevronLeft, ChevronRight, Filter, Search, SlidersHorizontal, Sprout, X } from "lucide-react";
import { FormEvent, useCallback, useEffect, useState } from "react";

import { ProductCard } from "@/components/catalog/product-card";
import { getApiErrorMessage } from "@/lib/api-error";
import { getPublicCategories, getPublicProducts } from "@/services/catalog-service";
import type { Category, PageData, Product, ProductFilters, ProductStatus } from "@/types/catalog";

const emptyPage: PageData<Product> = {
  content: [], page: 0, size: 12, totalElements: 0, totalPages: 0, first: true, last: true
};

type ProductBrowserProps = {
  initialSearch?: string;
  initialCategory?: string;
};

export function ProductBrowser({ initialSearch = "", initialCategory = "" }: ProductBrowserProps) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<PageData<Product>>(emptyPage);
  const [searchInput, setSearchInput] = useState(initialSearch);
  const [search, setSearch] = useState(initialSearch);
  const [category, setCategory] = useState(initialCategory);
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [status, setStatus] = useState<"" | Exclude<ProductStatus, "hidden">>("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getPublicCategories().then(setCategories).catch(() => undefined);
  }, []);

  const loadProducts = useCallback(async () => {
    setLoading(true);
    setError(null);
    const filters: ProductFilters = {
      search,
      category,
      minPrice: minPrice ? Number(minPrice) : undefined,
      maxPrice: maxPrice ? Number(maxPrice) : undefined,
      status: status || undefined,
      page,
      size: 12,
      sort: "createdAt,desc"
    };
    try {
      setProducts(await getPublicProducts(filters));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [category, maxPrice, minPrice, page, search, status]);

  useEffect(() => {
    const timer = window.setTimeout(() => { void loadProducts(); }, 0);
    return () => window.clearTimeout(timer);
  }, [loadProducts]);

  const applySearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  const resetFilters = () => {
    setSearchInput("");
    setSearch("");
    setCategory("");
    setMinPrice("");
    setMaxPrice("");
    setStatus("");
    setPage(0);
  };

  const activeCategory = categories.find((item) => item.slug === category);

  return (
    <main className="catalog-main product-browser-main">
      <header className="browser-heading">
        <span className="eyebrow">Catalog GreenFarm</span>
        <h1>{activeCategory?.name ?? (initialCategory ? "Sản phẩm theo danh mục" : "Tất cả sản phẩm")}</h1>
        <p>{activeCategory?.description ?? "Tìm thực phẩm phù hợp theo danh mục, khoảng giá và tình trạng hàng."}</p>
      </header>

      <div className="browser-layout">
        <aside className="filter-panel">
          <div className="filter-title"><SlidersHorizontal size={19} /><strong>Bộ lọc</strong></div>
          <form className="filter-stack" onSubmit={(event) => { event.preventDefault(); setPage(0); void loadProducts(); }}>
            <label>
              <span>Danh mục</span>
              <select value={category} onChange={(event) => { setCategory(event.target.value); setPage(0); }}>
                <option value="">Tất cả danh mục</option>
                {categories.map((item) => <option value={item.slug} key={item.id}>{item.name}</option>)}
              </select>
            </label>
            <div className="price-filter-row">
              <label><span>Giá từ</span><input type="number" min="0" value={minPrice} onChange={(event) => setMinPrice(event.target.value)} placeholder="0" /></label>
              <label><span>Đến</span><input type="number" min="0" value={maxPrice} onChange={(event) => setMaxPrice(event.target.value)} placeholder="500.000" /></label>
            </div>
            <label>
              <span>Trạng thái</span>
              <select value={status} onChange={(event) => { setStatus(event.target.value as typeof status); setPage(0); }}>
                <option value="">Tất cả</option>
                <option value="in_stock">Còn hàng</option>
                <option value="out_of_stock">Hết hàng</option>
              </select>
            </label>
            <button type="button" className="filter-apply-button" onClick={() => { setPage(0); void loadProducts(); }}><Filter size={17} /> Áp dụng</button>
            <button type="button" className="filter-reset-button" onClick={resetFilters}><X size={16} /> Xóa bộ lọc</button>
          </form>
        </aside>

        <section className="browser-results">
          <form className="catalog-searchbar" onSubmit={applySearch}>
            <Search size={19} aria-hidden="true" />
            <input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Tìm tên hoặc mô tả sản phẩm" aria-label="Tìm kiếm sản phẩm" />
            <button type="submit">Tìm</button>
          </form>
          <div className="result-summary">
            <span>{loading ? "Đang tải..." : `${products.totalElements} sản phẩm`}</span>
            {search && <span className="active-filter-chip">“{search}”</span>}
          </div>

          {error && <p className="catalog-notice error">{error}</p>}
          {loading ? (
            <div className="product-grid catalog-skeleton-grid" aria-label="Đang tải sản phẩm" />
          ) : products.content.length > 0 ? (
            <div className="product-grid browser-product-grid">
              {products.content.map((product) => <ProductCard product={product} key={product.id} />)}
            </div>
          ) : !error ? (
            <div className="catalog-empty"><Sprout size={34} /><h2>Chưa tìm thấy sản phẩm</h2><p>Hãy thử từ khóa hoặc khoảng giá khác.</p></div>
          ) : null}

          {products.totalPages > 1 && (
            <nav className="pagination" aria-label="Phân trang sản phẩm">
              <button type="button" disabled={products.first || loading} onClick={() => setPage((value) => Math.max(0, value - 1))}><ChevronLeft size={18} /> Trước</button>
              <span>Trang {products.page + 1} / {products.totalPages}</span>
              <button type="button" disabled={products.last || loading} onClick={() => setPage((value) => value + 1)}>Sau <ChevronRight size={18} /></button>
            </nav>
          )}
        </section>
      </div>
    </main>
  );
}
