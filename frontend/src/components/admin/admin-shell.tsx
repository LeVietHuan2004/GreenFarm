"use client";

import {
  Boxes,
  ChevronRight,
  CircleHelp,
  ExternalLink,
  LayoutDashboard,
  Leaf,
  LogOut,
  MessageCircleMore,
  Menu,
  Search,
  Settings,
  ShoppingCart,
  Store,
  Tags,
  TicketPercent,
  Truck,
  UserRound,
  UsersRound,
  X
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, ReactNode, useState } from "react";

import { RoleGuard } from "@/components/auth/role-guard";
import { NotificationMenu } from "@/components/notifications/notification-menu";
import { useAuthStore } from "@/stores/auth-store";

export type AdminSection = "dashboard" | "users" | "categories" | "products" | "orders" | "coupons" | "contacts";

type AdminShellProps = {
  active: AdminSection;
  children: ReactNode;
};

const ADMIN_ROLES = ["admin"] as const;

const sectionTitles: Record<AdminSection, { title: string; description: string }> = {
  dashboard: { title: "Dashboard", description: "Tổng quan hoạt động GreenFarm" },
  users: { title: "Người dùng", description: "Tài khoản, vai trò và trạng thái" },
  categories: { title: "Danh mục", description: "Cấu trúc catalog cửa hàng" },
  products: { title: "Sản phẩm", description: "Nội dung, tồn kho và hình ảnh" },
  orders: { title: "Đơn hàng", description: "Xử lý đơn và trạng thái thanh toán" },
  coupons: { title: "Mã giảm giá", description: "Ưu đãi, thời hạn và lượt sử dụng" },
  contacts: { title: "Liên hệ", description: "Yêu cầu hỗ trợ của khách hàng" }
};

const primaryItems = [
  { key: "dashboard" as const, label: "Dashboard", detail: "Tổng quan vận hành", href: "/admin", icon: LayoutDashboard },
  { key: "users" as const, label: "Người dùng", detail: "Khách hàng, nhân viên", href: "/admin/users", icon: UsersRound },
  { key: "categories" as const, label: "Danh mục", detail: "Nhóm sản phẩm", href: "/admin/categories", icon: Tags },
  { key: "products" as const, label: "Sản phẩm", detail: "Giá, kho và hình ảnh", href: "/admin/products", icon: Boxes },
  { key: "orders" as const, label: "Đơn hàng", detail: "Xử lý và giao nhận", href: "/admin/orders", icon: ShoppingCart },
  { key: "coupons" as const, label: "Mã giảm giá", detail: "Ưu đãi cửa hàng", href: "/admin/coupons", icon: TicketPercent },
  { key: "contacts" as const, label: "Liên hệ", detail: "Hỗ trợ khách hàng", href: "/admin/contacts", icon: MessageCircleMore }
];

const futureItems = [
  { label: "Giao hàng", detail: "Bảng điều phối", icon: Truck }
];

export function AdminShell({ active, children }: AdminShellProps) {
  const router = useRouter();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [quickSearch, setQuickSearch] = useState("");
  const { user, clearSession } = useAuthStore();
  const section = sectionTitles[active];

  const logout = () => {
    clearSession();
    router.push("/admin/login");
  };

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const query = quickSearch.trim();
    router.push(query ? `/admin/products?search=${encodeURIComponent(query)}` : "/admin/products");
  };

  return (
    <RoleGuard allowedRoles={ADMIN_ROLES} loginHref="/admin/login">
      <div className="admin-workspace">
        <aside className={`admin-sidebar ${sidebarOpen ? "open" : ""}`}>
          <div className="admin-sidebar-brand">
            <Link href="/admin" onClick={() => setSidebarOpen(false)}>
              <span><Leaf size={21} aria-hidden="true" /></span>
              <div><strong>GreenFarm</strong><small>Quản trị cửa hàng</small></div>
            </Link>
            <button type="button" className="admin-sidebar-close" onClick={() => setSidebarOpen(false)} aria-label="Đóng menu"><X size={19} /></button>
          </div>

          <div className="admin-sidebar-stage">
            <span>GF</span>
            <div><strong>Cửa hàng đang hoạt động</strong><small>Đơn hàng · Thanh toán</small></div>
          </div>

          <nav className="admin-sidebar-nav" aria-label="Điều hướng quản trị">
            <span className="admin-nav-label">Quản lý chính</span>
            {primaryItems.map(({ key, label, detail, href, icon: Icon }) => (
              <Link
                href={href}
                className={active === key ? "active" : ""}
                aria-current={active === key ? "page" : undefined}
                onClick={() => setSidebarOpen(false)}
                key={key}
              >
                <span className="admin-nav-icon"><Icon size={18} /></span>
                <span><strong>{label}</strong><small>{detail}</small></span>
                <ChevronRight className="admin-nav-arrow" size={15} />
              </Link>
            ))}

            <span className="admin-nav-label secondary">Sắp triển khai</span>
            {futureItems.map(({ label, detail, icon: Icon }) => (
              <button type="button" disabled key={label}>
                <span className="admin-nav-icon"><Icon size={18} /></span>
                <span><strong>{label}</strong><small>{detail}</small></span>
              </button>
            ))}
          </nav>

          <div className="admin-sidebar-footer">
            <Link href="/profile"><Settings size={17} /><span>Cài đặt tài khoản</span></Link>
            <Link href="/" target="_blank"><Store size={17} /><span>Xem cửa hàng</span><ExternalLink size={13} /></Link>
            <div className="admin-sidebar-user">
              <span className="admin-user-avatar"><UserRound size={19} /></span>
              <div><strong>{user?.name}</strong><small>{user?.email}</small></div>
              <button type="button" onClick={logout} aria-label="Đăng xuất" title="Đăng xuất"><LogOut size={17} /></button>
            </div>
          </div>
        </aside>

        {sidebarOpen && <button type="button" className="admin-sidebar-overlay" onClick={() => setSidebarOpen(false)} aria-label="Đóng menu" />}

        <div className="admin-workspace-content">
          <header className="admin-topbar">
            <button type="button" className="admin-menu-button" onClick={() => setSidebarOpen(true)} aria-label="Mở menu"><Menu size={21} /></button>
            <div className="admin-topbar-title"><strong>{section.title}</strong><span>{section.description}</span></div>
            <form className="admin-global-search" onSubmit={submitSearch}>
              <Search size={17} aria-hidden="true" />
              <input value={quickSearch} onChange={(event) => setQuickSearch(event.target.value)} placeholder="Tìm nhanh sản phẩm..." aria-label="Tìm nhanh sản phẩm" />
            </form>
            <button type="button" className="admin-topbar-icon" aria-label="Trợ giúp" title="Trợ giúp"><CircleHelp size={18} /></button>
            <NotificationMenu admin />
            <div className="admin-topbar-account">
              <span><UserRound size={18} /></span>
              <div><strong>{user?.name}</strong><small>Quản trị viên</small></div>
            </div>
          </header>
          <div className="admin-page-scroll">{children}</div>
        </div>
      </div>
    </RoleGuard>
  );
}
