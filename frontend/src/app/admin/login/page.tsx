import type { Metadata } from "next";

import { RoleLoginPage } from "@/components/auth/role-login-page";
import { getPortalById } from "@/config/login-portals";

export const metadata: Metadata = { title: "Đăng nhập quản trị" };

export default function AdminLoginPage() {
  return <RoleLoginPage portal={getPortalById("admin")} />;
}
