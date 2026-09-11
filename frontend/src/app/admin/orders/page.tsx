import type { Metadata } from "next";

import { AdminOrders } from "@/components/admin/admin-orders";

export const metadata: Metadata = { title: "Quản lý đơn hàng" };

export default function AdminOrdersPage() {
  return <AdminOrders />;
}
