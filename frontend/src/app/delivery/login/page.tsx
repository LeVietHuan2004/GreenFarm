import type { Metadata } from "next";

import { RoleLoginPage } from "@/components/auth/role-login-page";
import { getPortalById } from "@/config/login-portals";

export const metadata: Metadata = { title: "Đăng nhập giao hàng" };

export default function DeliveryLoginPage() {
  return <RoleLoginPage portal={getPortalById("delivery")} />;
}
