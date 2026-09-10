"use client";

import { ArrowRight, ShieldCheck, UserRound } from "lucide-react";
import Link from "next/link";

import { getRoleHome } from "@/config/login-portals";
import { useAuthStore } from "@/stores/auth-store";

export function AccountPanel() {
  const { user, hasHydrated } = useAuthStore();

  if (!hasHydrated) {
    return <div className="account-panel loading-panel" aria-label="Đang tải tài khoản" />;
  }

  if (!user) {
    return (
      <section className="account-panel">
        <div className="panel-icon tomato"><UserRound size={24} aria-hidden="true" /></div>
        <div className="panel-copy">
          <span className="eyebrow">Tài khoản</span>
          <h2>Bắt đầu với GreenFarm</h2>
          <p>Đăng nhập bằng tài khoản hiện có hoặc tạo một tài khoản khách hàng mới.</p>
        </div>
        <div className="panel-actions">
          <Link href="/login" className="primary-button">
            Đăng nhập <ArrowRight size={17} aria-hidden="true" />
          </Link>
          <Link href="/register" className="secondary-button">Đăng ký</Link>
        </div>
      </section>
    );
  }

  return (
    <section className="account-panel">
      <div className="panel-icon"><ShieldCheck size={24} aria-hidden="true" /></div>
      <div className="panel-copy">
        <span className="eyebrow">Đã xác thực</span>
        <h2>Xin chào, {user.name}</h2>
        <p>{user.email} · Vai trò {user.role}</p>
      </div>
      <div className="panel-actions">
        <Link href={getRoleHome(user.role)} className="primary-button">
          Mở khu vực <ArrowRight size={17} aria-hidden="true" />
        </Link>
      </div>
    </section>
  );
}
