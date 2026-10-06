"use client";

import { Eye, EyeOff, ImagePlus, PackagePlus, Pencil, RotateCcw, Save, Search, Trash2 } from "lucide-react";
import { FormEvent, useCallback, useEffect, useState } from "react";
import Link from "next/link";

import { AdminShell } from "@/components/admin/admin-shell";
import { CatalogImage } from "@/components/catalog/catalog-image";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice, primaryProductImage, productStatusLabel } from "@/lib/catalog-format";
import {
  createProduct,
  deleteProductImage,
  getAdminCategories,
  getAdminProducts,
  updateProduct,
  updateProductStatus,
  uploadProductImage
} from "@/services/catalog-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Category, Product, ProductInput, ProductStatus } from "@/types/catalog";

type ProductForm = Omit<ProductInput, "categoryId" | "price" | "stock"> & {
  categoryId: string;
  price: string;
  stock: string;
  image: File | null;
};

const emptyForm: ProductForm = {
  name: "", nameEn: "", slug: "", categoryId: "", description: "", descriptionEn: "",
  price: "", stock: "0", status: "in_stock", unit: "kg", unitEn: "", image: null
};

export function AdminProducts({ initialSearch = "" }: { initialSearch?: string }) {
  const adminReady = useAuthStore((state) => state.hasHydrated
    && Boolean(state.token)
    && state.user?.role === "admin");
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState<ProductForm>(emptyForm);
  const [editing, setEditing] = useState<Product | null>(null);
  const [search, setSearch] = useState(initialSearch);
  const [statusFilter, setStatusFilter] = useState<"" | ProductStatus>("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadProducts = useCallback(async () => {
    setLoading(true);
    try {
      const page = await getAdminProducts({
        search,
        status: statusFilter || undefined,
        categoryId: categoryFilter ? Number(categoryFilter) : undefined,
        size: 50,
        sort: "updatedAt,desc"
      });
      setProducts(page.content);
      setError(null);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [categoryFilter, search, statusFilter]);

  useEffect(() => {
    if (!adminReady) return;
    getAdminCategories().then((data) => {
      setCategories(data);
      setForm((current) => current.categoryId || data.length === 0
        ? current
        : { ...current, categoryId: String(data[0].id) });
    }).catch((requestError) => setError(getApiErrorMessage(requestError)));
  }, [adminReady]);

  useEffect(() => {
    if (!adminReady) return;
    const timer = window.setTimeout(() => { void loadProducts(); }, 0);
    return () => window.clearTimeout(timer);
  }, [adminReady, loadProducts]);

  const reset = () => {
    setEditing(null);
    setForm({ ...emptyForm, categoryId: categories[0] ? String(categories[0].id) : "" });
  };

  const edit = (product: Product) => {
    setEditing(product);
    setForm({
      name: product.name,
      nameEn: product.nameEn ?? "",
      slug: product.slug,
      categoryId: String(product.category.id),
      description: product.description ?? "",
      descriptionEn: product.descriptionEn ?? "",
      price: String(product.price),
      stock: String(product.stock),
      status: product.status,
      unit: product.unit ?? "",
      unitEn: product.unitEn ?? "",
      image: null
    });
    setMessage(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!form.categoryId) {
      setError("Hãy tạo ít nhất một danh mục trước khi thêm sản phẩm.");
      return;
    }
    setSaving(true);
    setError(null);
    setMessage(null);
    const input: ProductInput = {
      name: form.name,
      nameEn: form.nameEn,
      slug: form.slug,
      categoryId: Number(form.categoryId),
      description: form.description,
      descriptionEn: form.descriptionEn,
      price: Number(form.price),
      stock: Number(form.stock),
      status: form.status,
      unit: form.unit,
      unitEn: form.unitEn
    };
    try {
      const saved = editing
        ? await updateProduct(editing.id, input)
        : await createProduct(input);
      if (form.image) await uploadProductImage(saved.id, form.image);
      setMessage(editing ? "Đã cập nhật sản phẩm." : "Đã tạo sản phẩm mới.");
      reset();
      await loadProducts();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  };

  const toggleVisibility = async (product: Product) => {
    const status: ProductStatus = product.status === "hidden"
      ? (product.stock > 0 ? "in_stock" : "out_of_stock")
      : "hidden";
    try {
      await updateProductStatus(product.id, status);
      setMessage(status === "hidden" ? "Đã ẩn sản phẩm khỏi catalog." : "Đã hiển thị lại sản phẩm.");
      await loadProducts();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    }
  };

  const removeImage = async (productId: number, imageId: number) => {
    if (!window.confirm("Xóa ảnh này khỏi sản phẩm?")) return;
    try {
      await deleteProductImage(productId, imageId);
      setProducts((current) => current.map((product) => product.id === productId
        ? { ...product, images: product.images.filter((image) => image.id !== imageId) }
        : product));
      setEditing((current) => current?.id === productId
        ? { ...current, images: current.images.filter((image) => image.id !== imageId) }
        : current);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    }
  };

  return (
    <AdminShell active="products">
      <main className="admin-catalog-main">
          <header className="admin-page-heading">
            <div><span className="eyebrow">Catalog · Quản trị</span><h1>Sản phẩm</h1><p>Quản lý nội dung, giá, tồn kho, trạng thái hiển thị và hình ảnh.</p></div>
            <span className="admin-count">{products.length} mục đang xem</span>
          </header>

          <section className="admin-editor-card">
            <div className="admin-editor-heading">
              <span><PackagePlus size={20} /></span>
              <div><h2>{editing ? `Chỉnh sửa ${editing.name}` : "Thêm sản phẩm"}</h2><p>Slug để trống sẽ được tạo tự động; ảnh tải lên hỗ trợ JPG, PNG hoặc WEBP.</p></div>
            </div>
            <form className="admin-form-grid product-admin-form" onSubmit={submit}>
              <label><span>Tên sản phẩm *</span><input required maxLength={255} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
              <label><span>Danh mục *</span><select required value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}><option value="">Chọn danh mục</option>{categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}</select></label>
              <label><span>Giá (VND) *</span><input required type="number" min="0" step="100" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} /></label>
              <label><span>Tồn kho</span><input readOnly type="number" value={form.stock} /><small>Thay đổi số lượng tại <Link href="/admin/inventory">Quản lý kho</Link>.</small></label>
              <label><span>Đơn vị</span><input maxLength={255} value={form.unit} onChange={(event) => setForm({ ...form, unit: event.target.value })} placeholder="kg, túi, hộp..." /></label>
              <label><span>Trạng thái</span><select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value as ProductStatus })}><option value="in_stock">Còn hàng</option><option value="out_of_stock">Hết hàng</option><option value="hidden">Ẩn</option></select></label>
              <label><span>Slug</span><input maxLength={255} value={form.slug} onChange={(event) => setForm({ ...form, slug: event.target.value })} placeholder="tu-dong-tao" /></label>
              <label><span>{editing ? "Thêm ảnh mới" : "Ảnh đầu tiên"}</span><input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => setForm({ ...form, image: event.target.files?.[0] ?? null })} /></label>
              <label className="admin-form-wide"><span>Mô tả</span><textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
              {editing && editing.images.length > 0 && (
                <div className="admin-form-wide image-manager">
                  <span>Ảnh hiện tại</span>
                  <div>{editing.images.map((image) => <figure key={image.id}><CatalogImage src={image.image} alt={editing.name} /><button type="button" onClick={() => void removeImage(editing.id, image.id)} aria-label="Xóa ảnh"><Trash2 size={15} /></button></figure>)}</div>
                </div>
              )}
              <div className="admin-form-actions">
                <button className="primary-button" disabled={saving || categories.length === 0} type="submit"><Save size={17} /> {saving ? "Đang lưu..." : editing ? "Lưu thay đổi" : "Tạo sản phẩm"}</button>
                {editing && <button className="secondary-button" type="button" onClick={reset}><RotateCcw size={17} /> Hủy sửa</button>}
              </div>
            </form>
          </section>

          {error && <p className="catalog-notice error">{error}</p>}
          {message && <p className="catalog-notice success">{message}</p>}

          <section className="admin-list-card">
            <div className="admin-toolbar">
              <form onSubmit={(event) => { event.preventDefault(); void loadProducts(); }}><Search size={18} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tìm sản phẩm" /><button type="submit">Tìm</button></form>
              <select value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}><option value="">Mọi danh mục</option>{categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}</select>
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as typeof statusFilter)}><option value="">Mọi trạng thái</option><option value="in_stock">Còn hàng</option><option value="out_of_stock">Hết hàng</option><option value="hidden">Đang ẩn</option></select>
            </div>
            <div className="admin-product-list">
              {products.map((product) => (
                <article className={`admin-product-row ${product.status === "hidden" ? "is-hidden" : ""}`} key={product.id}>
                  <div className="admin-product-image"><CatalogImage src={primaryProductImage(product)} alt={product.name} /></div>
                  <div className="admin-row-copy"><span className="category-kicker">{product.category.name}</span><strong>{product.name}</strong><p>{formatPrice(product.price)} / {product.unit ?? "sản phẩm"} · Tồn {product.stock}</p></div>
                  <span className={`stock-badge static ${product.status}`}>{productStatusLabel[product.status]}</span>
                  <span className="image-count"><ImagePlus size={15} /> {product.images.length}</span>
                  <div className="admin-row-actions">
                    <button type="button" onClick={() => edit(product)}><Pencil size={16} /> Sửa</button>
                    <button type="button" onClick={() => void toggleVisibility(product)}>{product.status === "hidden" ? <Eye size={16} /> : <EyeOff size={16} />}{product.status === "hidden" ? "Hiện" : "Ẩn"}</button>
                  </div>
                </article>
              ))}
              {!loading && products.length === 0 && <div className="admin-empty">Không có sản phẩm phù hợp bộ lọc.</div>}
              {loading && <div className="admin-empty">Đang tải sản phẩm...</div>}
            </div>
          </section>
      </main>
    </AdminShell>
  );
}
