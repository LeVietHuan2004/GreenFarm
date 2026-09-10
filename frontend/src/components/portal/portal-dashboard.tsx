"use client";

import {
  Boxes,
  ClipboardList,
  PackageCheck,
  PackageOpen,
  ShoppingBasket,
  Truck,
  UserRound,
  UsersRound
} from "lucide-react";
import Link from "next/link";

import { RoleGuard } from "@/components/auth/role-guard";
import { SiteHeader } from "@/components/layout/site-header";
import { useAuthStore } from "@/stores/auth-store";
import type { UserRole } from "@/types/auth";

type PortalKind = "admin" | "delivery";

const portalContent = {
  admin: {
    role: "admin" as UserRole,
    loginHref: "/admin/login",
    eyebrow: "Hệ thống quản lý",
    title: "Bảng quản trị cửa hàng",
    description: "Bộ khung điều hành dành riêng cho quản trị viên.",
    modules: [
      { label: "Người dùng", detail: "Tài khoản và vai trò", icon: UsersRound },
      { label: "Sản phẩm", detail: "Danh sách, tồn kho và hình ảnh", icon: Boxes, href: "/admin/products" },
      { label: "Danh mục", detail: "Nhóm sản phẩm cửa hàng", icon: ShoppingBasket, href: "/admin/categories" },
      { label: "Đơn hàng", detail: "Theo dõi và xử lý", icon: ClipboardList }
    ]
  },
  delivery: {
    role: "delivery_staff" as UserRole,
    loginHref: "/delivery/login",
    eyebrow: "Khu vực giao hàng",
    title: "Bảng công việc giao nhận",
    description: "Bộ khung tác nghiệp dành riêng cho nhân viên giao hàng.",
    modules: [
      { label: "Đơn cần nhận", detail: "Danh sách chờ bàn giao", icon: PackageOpen },
      { label: "Đang giao", detail: "Các chuyến đang thực hiện", icon: Truck },
      { label: "Đã hoàn tất", detail: "Lịch sử giao hàng", icon: PackageCheck }
    ]
  }
} as const;

export function PortalDashboard({ kind }: { kind: PortalKind }) {
  const content = portalContent[kind];
  const user = useAuthStore((state) => state.user);

  return (
    <RoleGuard allowedRoles={[content.role]} loginHref={content.loginHref}>
      <div className={`app-shell portal-shell portal-${kind}`}>
        <SiteHeader />
        <main className="portal-main">
          <header className="portal-heading">
            <div>
              <span className="eyebrow">{content.eyebrow}</span>
              <h1>{content.title}</h1>
              <p>{content.description}</p>
            </div>
            <div className="portal-identity">
              <UserRound size={18} aria-hidden="true" />
              <span>{user?.name}</span>
            </div>
          </header>

          <section className="portal-module-grid" aria-label="Các module chính">
            {content.modules.map(({ label, detail, icon: Icon, ...module }) => {
              const body = (
                <>
                  <span className="portal-module-icon"><Icon size={22} aria-hidden="true" /></span>
                  <div><h2>{label}</h2><p>{detail}</p></div>
                  <span className="module-state">Bộ khung</span>
                </>
              );

              return "href" in module && module.href
                ? <Link className="portal-module" href={module.href} key={label}>{body}</Link>
                : <article className="portal-module" key={label}>{body}</article>;
            })}
          </section>

          <section className="portal-empty-state">
            <ClipboardList size={24} aria-hidden="true" />
            <div>
              <h2>Catalog sản phẩm đã sẵn sàng</h2>
              <p>Danh mục, sản phẩm và hình ảnh đã được kết nối với dữ liệu GreenFarm. Các module đơn hàng sẽ tiếp tục ở giai đoạn sau.</p>
            </div>
          </section>
        </main>
      </div>
    </RoleGuard>
  );
}
