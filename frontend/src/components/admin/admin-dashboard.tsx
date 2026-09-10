"use client";

import {
  ArrowRight,
  Boxes,
  CheckCircle2,
  CircleAlert,
  EyeOff,
  PackageCheck,
  ShoppingBasket,
  Sparkles,
  Sprout,
  Tags,
  UsersRound
} from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { AdminShell } from "@/components/admin/admin-shell";
import { CatalogImage } from "@/components/catalog/catalog-image";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice, primaryProductImage, productStatusLabel } from "@/lib/catalog-format";
import { getAdminUsers } from "@/services/admin-service";
import { getAdminCategories, getAdminProducts } from "@/services/catalog-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Product } from "@/types/catalog";

type DashboardData = {
  products: number;
  visibleProducts: number;
  outOfStock: number;
  hiddenProducts: number;
  categories: number;
  users: number;
  activeUsers: number;
  recentProducts: Product[];
};

const emptyDashboard: DashboardData = {
  products: 0,
  visibleProducts: 0,
  outOfStock: 0,
  hiddenProducts: 0,
  categories: 0,
  users: 0,
  activeUsers: 0,
  recentProducts: []
};

const dashboardModules = [
  { href: "/admin/users", label: "Người dùng", detail: "Tài khoản, vai trò và trạng thái truy cập", icon: UsersRound, tone: "mint", badge: "Quản lý" },
  { href: "/admin/categories", label: "Danh mục", detail: "Tổ chức các nhóm sản phẩm trên cửa hàng", icon: Tags, tone: "sky", badge: "Catalog" },
  { href: "/admin/products", label: "Sản phẩm", detail: "Giá bán, tồn kho, trạng thái và hình ảnh", icon: Boxes, tone: "peach", badge: "Catalog" },
  { href: "/products", label: "Cửa hàng", detail: "Kiểm tra catalog đang hiển thị cho khách", icon: ShoppingBasket, tone: "lavender", badge: "Xem nhanh" }
] as const;

export function AdminDashboard() {
  const { user, token, hasHydrated } = useAuthStore();
  const [data, setData] = useState<DashboardData>(emptyDashboard);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!hasHydrated || !token || user?.role !== "admin") return;
    let active = true;
    Promise.all([
      getAdminProducts({ size: 6, sort: "updatedAt,desc" }),
      getAdminProducts({ status: "out_of_stock", size: 1 }),
      getAdminProducts({ status: "hidden", size: 1 }),
      getAdminCategories(),
      getAdminUsers({ size: 1 }),
      getAdminUsers({ status: "active", size: 1 })
    ])
      .then(([products, outOfStock, hidden, categories, users, activeUsers]) => {
        if (!active) return;
        setData({
          products: products.totalElements,
          visibleProducts: Math.max(0, products.totalElements - hidden.totalElements),
          outOfStock: outOfStock.totalElements,
          hiddenProducts: hidden.totalElements,
          categories: categories.length,
          users: users.totalElements,
          activeUsers: activeUsers.totalElements,
          recentProducts: products.content
        });
      })
      .catch((requestError) => active && setError(getApiErrorMessage(requestError)))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [hasHydrated, token, user?.role]);

  const visibilityRate = useMemo(() => data.products === 0
    ? 0
    : Math.round((data.visibleProducts / data.products) * 100), [data.products, data.visibleProducts]);

  const metrics = [
    { label: "Người dùng", value: data.users, detail: `${data.activeUsers} đang hoạt động`, icon: UsersRound, tone: "amber" },
    { label: "Danh mục", value: data.categories, detail: "Đang sử dụng", icon: Tags, tone: "rose" },
    { label: "Sản phẩm", value: data.products, detail: `${data.visibleProducts} đang hiển thị`, icon: Boxes, tone: "green" },
    { label: "Cần xử lý", value: data.outOfStock, detail: "Sản phẩm hết hàng", icon: CircleAlert, tone: "blue" }
  ];

  return (
    <AdminShell active="dashboard">
      <main className="admin-dashboard-main">
        <section className="admin-welcome">
          <div>
            <span className="admin-welcome-pill"><Sparkles size={14} /> Trung tâm vận hành</span>
            <h1>Xin chào, {user?.name?.split(" ").at(-1) ?? "Admin"}</h1>
            <p>Đây là tình hình catalog và tài khoản GreenFarm hôm nay.</p>
          </div>
          <Link href="/admin/products" className="admin-welcome-action"><Boxes size={17} /> Quản lý sản phẩm <ArrowRight size={16} /></Link>
        </section>

        {error && <p className="catalog-notice error">{error}</p>}

        <section className="admin-metric-grid" aria-label="Chỉ số tổng quan">
          {metrics.map(({ label, value, detail, icon: Icon, tone }) => (
            <article className="admin-metric-card" key={label}>
              <div className="admin-metric-label"><span>{label}</span><i className={tone}><Icon size={18} /></i></div>
              <strong>{loading ? "—" : value.toLocaleString("vi-VN")}</strong>
              <small>{detail}</small>
            </article>
          ))}
        </section>

        <section className="admin-dashboard-section">
          <div className="admin-dashboard-section-heading">
            <div><span className="eyebrow">Truy cập nhanh</span><h2>Không gian quản lý</h2></div>
            <span>Các module đang hoạt động</span>
          </div>
          <div className="admin-dashboard-module-grid">
            {dashboardModules.map(({ href, label, detail, icon: Icon, tone, badge }) => (
              <Link href={href} className={`admin-dashboard-module ${tone}`} key={label}>
                <div className="admin-module-art" aria-hidden="true">
                  <span><Icon size={38} /></span><i /><i />
                </div>
                <span className="admin-module-badge">{badge}</span>
                <div><h3>{label}</h3><p>{detail}</p></div>
                <ArrowRight size={17} className="admin-module-arrow" />
              </Link>
            ))}
          </div>
        </section>

        <section className="admin-dashboard-bottom">
          <article className="admin-operations-card">
            <div className="admin-card-heading"><div><span className="eyebrow">Tình trạng</span><h2>Sức khỏe catalog</h2></div><PackageCheck size={21} /></div>
            <div className="admin-health-score"><strong>{loading ? "—" : `${visibilityRate}%`}</strong><span>Sản phẩm đang hiển thị công khai</span></div>
            <div className="admin-health-track"><span style={{ width: `${visibilityRate}%` }} /></div>
            <div className="admin-health-list">
              <div><span><CheckCircle2 size={16} /> Đang hiển thị</span><strong>{data.visibleProducts}</strong></div>
              <div><span><CircleAlert size={16} /> Hết hàng</span><strong>{data.outOfStock}</strong></div>
              <div><span><EyeOff size={16} /> Đang ẩn</span><strong>{data.hiddenProducts}</strong></div>
            </div>
            <Link href="/admin/products">Xem toàn bộ sản phẩm <ArrowRight size={15} /></Link>
          </article>

          <article className="admin-recent-card">
            <div className="admin-card-heading"><div><span className="eyebrow">Cập nhật gần đây</span><h2>Sản phẩm mới chỉnh sửa</h2></div><Sprout size={21} /></div>
            <div className="admin-recent-list">
              {data.recentProducts.slice(0, 5).map((product) => (
                <Link href="/admin/products" key={product.id}>
                  <span className="admin-recent-image"><CatalogImage src={primaryProductImage(product)} alt={product.name} /></span>
                  <span><strong>{product.name}</strong><small>{formatPrice(product.price)} · {productStatusLabel[product.status]}</small></span>
                  <ArrowRight size={15} />
                </Link>
              ))}
              {!loading && data.recentProducts.length === 0 && <div className="admin-empty">Chưa có sản phẩm.</div>}
            </div>
          </article>
        </section>
      </main>
    </AdminShell>
  );
}
