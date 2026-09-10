import type { Metadata } from "next";

import { AdminDashboard } from "@/components/admin/admin-dashboard";

export const metadata: Metadata = { title: "Quản trị cửa hàng" };

export default function AdminPage() {
  return <AdminDashboard />;
}
