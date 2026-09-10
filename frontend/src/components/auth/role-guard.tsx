"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { getRoleHome } from "@/config/login-portals";
import { useAuthStore } from "@/stores/auth-store";
import type { UserRole } from "@/types/auth";

type RoleGuardProps = {
  allowedRoles: readonly UserRole[];
  loginHref: string;
  children: ReactNode;
};

export function RoleGuard({ allowedRoles, loginHref, children }: RoleGuardProps) {
  const router = useRouter();
  const { token, user, hasHydrated } = useAuthStore();
  const authorized = Boolean(token && user && allowedRoles.includes(user.role));

  useEffect(() => {
    if (!hasHydrated) return;
    if (!token || !user) {
      router.replace(loginHref);
      return;
    }
    if (!allowedRoles.includes(user.role)) {
      router.replace(getRoleHome(user.role));
    }
  }, [allowedRoles, hasHydrated, loginHref, router, token, user]);

  if (!hasHydrated || !authorized) {
    return (
      <main className="portal-loading" aria-live="polite">
        Đang kiểm tra quyền truy cập...
      </main>
    );
  }

  return children;
}
