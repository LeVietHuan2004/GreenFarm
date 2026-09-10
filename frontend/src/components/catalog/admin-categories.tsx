"use client";

import { FolderPlus, Image as ImageIcon, Pencil, RotateCcw, Save, Trash2 } from "lucide-react";
import { FormEvent, useCallback, useEffect, useState } from "react";

import { AdminShell } from "@/components/admin/admin-shell";
import { CatalogImage } from "@/components/catalog/catalog-image";
import { getApiErrorMessage } from "@/lib/api-error";
import {
  createCategory,
  deleteCategory,
  getAdminCategories,
  updateCategory
} from "@/services/catalog-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Category, CategoryInput } from "@/types/catalog";

const emptyForm: CategoryInput = {
  name: "", nameEn: "", slug: "", description: "", descriptionEn: "", image: ""
};

export function AdminCategories() {
  const adminReady = useAuthStore((state) => state.hasHydrated
    && Boolean(state.token)
    && state.user?.role === "admin");
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState<CategoryInput>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadCategories = useCallback(async () => {
    setLoading(true);
    try {
      setCategories(await getAdminCategories());
      setError(null);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!adminReady) return;
    const timer = window.setTimeout(() => { void loadCategories(); }, 0);
    return () => window.clearTimeout(timer);
  }, [adminReady, loadCategories]);

  const reset = () => {
    setEditingId(null);
    setForm(emptyForm);
    setError(null);
  };

  const edit = (category: Category) => {
    setEditingId(category.id);
    setForm({
      name: category.name,
      nameEn: category.nameEn ?? "",
      slug: category.slug,
      description: category.description ?? "",
      descriptionEn: category.descriptionEn ?? "",
      image: category.image ?? ""
    });
    setMessage(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      if (editingId) {
        await updateCategory(editingId, form);
        setMessage("Đã cập nhật danh mục.");
      } else {
        await createCategory(form);
        setMessage("Đã tạo danh mục mới.");
      }
      reset();
      await loadCategories();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (category: Category) => {
    if (!window.confirm(`Xóa danh mục “${category.name}”?`)) return;
    try {
      await deleteCategory(category.id);
      setMessage("Đã xóa danh mục.");
      await loadCategories();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    }
  };

  return (
    <AdminShell active="categories">
      <main className="admin-catalog-main">
          <header className="admin-page-heading">
            <div><span className="eyebrow">Catalog · Quản trị</span><h1>Danh mục sản phẩm</h1><p>Tổ chức các nhóm nông sản hiển thị trên cửa hàng.</p></div>
            <span className="admin-count">{categories.length} danh mục</span>
          </header>

          <section className="admin-editor-card">
            <div className="admin-editor-heading">
              <span><FolderPlus size={20} /></span>
              <div><h2>{editingId ? "Chỉnh sửa danh mục" : "Thêm danh mục"}</h2><p>Slug để trống sẽ được tạo tự động từ tên.</p></div>
            </div>
            <form className="admin-form-grid" onSubmit={submit}>
              <label><span>Tên danh mục *</span><input required maxLength={255} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
              <label><span>Tên tiếng Anh</span><input maxLength={255} value={form.nameEn} onChange={(event) => setForm({ ...form, nameEn: event.target.value })} /></label>
              <label><span>Slug</span><input maxLength={255} value={form.slug} onChange={(event) => setForm({ ...form, slug: event.target.value })} placeholder="tu-dong-tao" /></label>
              <label><span>URL hình ảnh</span><input maxLength={255} value={form.image} onChange={(event) => setForm({ ...form, image: event.target.value })} placeholder="https://..." /></label>
              <label className="admin-form-wide"><span>Mô tả</span><textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
              <div className="admin-form-actions">
                <button className="primary-button" disabled={saving} type="submit"><Save size={17} /> {saving ? "Đang lưu..." : editingId ? "Lưu thay đổi" : "Tạo danh mục"}</button>
                {editingId && <button className="secondary-button" type="button" onClick={reset}><RotateCcw size={17} /> Hủy sửa</button>}
              </div>
            </form>
          </section>

          {error && <p className="catalog-notice error">{error}</p>}
          {message && <p className="catalog-notice success">{message}</p>}

          <section className="admin-list-card">
            <div className="admin-list-heading"><h2>Danh sách danh mục</h2><span>{loading ? "Đang tải..." : "Dữ liệu đang sử dụng"}</span></div>
            <div className="admin-category-list">
              {categories.map((category) => (
                <article className="admin-category-row" key={category.id}>
                  <div className="admin-category-image"><CatalogImage src={category.image} alt={category.name} /></div>
                  <div className="admin-row-copy"><strong>{category.name}</strong><span>/{category.slug} · {category.productCount} sản phẩm</span><p>{category.description ?? "Chưa có mô tả"}</p></div>
                  <div className="admin-row-actions">
                    <button type="button" onClick={() => edit(category)}><Pencil size={16} /> Sửa</button>
                    <button type="button" className="danger" disabled={category.productCount > 0} onClick={() => void remove(category)} title={category.productCount > 0 ? "Danh mục đang có sản phẩm" : "Xóa danh mục"}><Trash2 size={16} /></button>
                  </div>
                </article>
              ))}
              {!loading && categories.length === 0 && <div className="admin-empty"><ImageIcon size={26} /> Chưa có danh mục.</div>}
            </div>
          </section>
      </main>
    </AdminShell>
  );
}
