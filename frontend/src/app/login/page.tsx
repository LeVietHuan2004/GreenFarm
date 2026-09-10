import type { Metadata } from "next";

import { RoleLoginPage } from "@/components/auth/role-login-page";
import { getPortalById } from "@/config/login-portals";

export const metadata: Metadata = { title: "Đăng nhập" };

export default function LoginPage() {
  return <RoleLoginPage portal={getPortalById("customer")} />;
}
