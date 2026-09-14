"use client";

import { Heart, LayoutDashboard, Leaf, LogOut, ReceiptText, Search, ShoppingBasket, UserRound } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";

import { RoleLoginMenu } from "@/components/auth/role-login-menu";
import { useCommerce } from "@/components/commerce/commerce-provider";
import { NotificationMenu } from "@/components/notifications/notification-menu";
import { getRoleHome, getRoleLogin } from "@/config/login-portals";
import { useAuthStore } from "@/stores/auth-store";

export function SiteHeader() {
  const router = useRouter();
  const { cart, wishlist, lastAction } = useCommerce();
  const { user, hasHydrated, clearSession } = useAuthStore();
  const [search, setSearch] = useState("");

  const logout = () => {
    const loginHref = getRoleLogin(user?.role);
    clearSession();
    router.push(loginHref);
  };

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const query = search.trim();
    router.push(query ? `/products?search=${encodeURIComponent(query)}` : "/products");
  };

  return (
    <header className="site-header">
      <div className="site-header-inner">
        <Link href="/" className="brand-link dark">
          <span className="brand-mark"><Leaf size={20} aria-hidden="true" /></span>
          <span className="brand-copy">
            <strong>GreenFarm</strong>
            <small>Nông sản tươi từ nông trại</small>
          </span>
        </Link>

        <form className="header-search" onSubmit={submitSearch}>
          <Search size={17} aria-hidden="true" />
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Tìm rau củ, trái cây, gạo sạch..."
            aria-label="Tìm kiếm sản phẩm"
          />
          <button type="submit">Tìm</button>
        </form>

        <nav className="site-primary-nav" aria-label="Cửa hàng">
          <Link href="/#categories">Danh mục</Link>
          <Link href="/products">Sản phẩm</Link>
          <Link href="/#delivery">Giao hàng</Link>
          <Link href="/contact">Liên hệ</Link>
        </nav>

        <nav className="account-nav" aria-label="Tài khoản">
          <Link key={`wishlist-${lastAction?.target === "wishlist" ? lastAction.id : 0}`} href="/wishlist"
            className={`header-icon-link commerce-header-link${lastAction?.target === "wishlist" ? " is-bumping" : ""}`}
            aria-label={`Sản phẩm yêu thích (${wishlist.count})`} title="Sản phẩm yêu thích">
            <Heart size={18} aria-hidden="true" />
            {wishlist.count > 0 && <span className="commerce-count">{wishlist.count > 99 ? "99+" : wishlist.count}</span>}
          </Link>
          <Link key={`cart-${lastAction?.target === "cart" ? lastAction.id : 0}`} href="/cart"
            className={`header-icon-link commerce-header-link${lastAction?.target === "cart" ? " is-bumping" : ""}`}
            aria-label={`Giỏ hàng (${cart.totalItems})`} title="Giỏ hàng">
            <ShoppingBasket size={18} aria-hidden="true" />
            {cart.totalItems > 0 && <span className="commerce-count">{cart.totalItems > 99 ? "99+" : cart.totalItems}</span>}
          </Link>
          {hasHydrated && user?.role === "customer" && <Link href="/orders" className="header-icon-link" aria-label="Đơn hàng" title="Đơn hàng"><ReceiptText size={18}/></Link>}
          {hasHydrated && user ? (
            <>
              <NotificationMenu />
              <Link href={getRoleHome(user.role)} className="secondary-button compact-button portal-link">
                <LayoutDashboard size={17} aria-hidden="true" />
                Khu vực
              </Link>
              <Link href="/profile" className="header-icon-link" aria-label="Hồ sơ" title="Hồ sơ">
                <UserRound size={17} aria-hidden="true" />
              </Link>
              <button type="button" className="header-icon-link" onClick={logout} aria-label="Đăng xuất" title="Đăng xuất">
                <LogOut size={18} aria-hidden="true" />
              </button>
            </>
          ) : (
            <RoleLoginMenu compact />
          )}
        </nav>
      </div>
    </header>
  );
}
