import { ShieldCheck, Truck, UserRound } from "lucide-react";
import Link from "next/link";

import { AuthShell } from "@/components/auth/auth-shell";
import { LoginForm } from "@/components/auth/login-form";
import { RoleLoginMenu } from "@/components/auth/role-login-menu";
import type { LoginPortal } from "@/config/login-portals";

type RoleLoginPageProps = {
  portal: LoginPortal;
};

export function RoleLoginPage({ portal }: RoleLoginPageProps) {
  const PortalIcon = portal.id === "admin"
    ? ShieldCheck
    : portal.id === "delivery"
      ? Truck
      : UserRound;

  return (
    <AuthShell
      title={portal.title}
      description={portal.description}
      visualEyebrow={portal.visualEyebrow}
      visualTitle={portal.visualTitle}
      topAction={<RoleLoginMenu currentPortal={portal.id} compact />}
      headingIcon={<PortalIcon size={20} aria-hidden="true" />}
      footer={portal.id === "customer"
        ? <><span>Chưa có tài khoản?</span> <Link href="/register">Tạo tài khoản</Link></>
        : <><span>Không đúng khu vực?</span> <Link href="/login">Chọn cổng khác</Link></>}
    >
      <LoginForm portal={portal} />
    </AuthShell>
  );
}
