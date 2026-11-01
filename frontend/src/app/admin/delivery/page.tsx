import type { Metadata } from "next";
import { AdminDelivery } from "@/components/admin/admin-delivery";

export const metadata: Metadata = { title: "Quản lý giao hàng" };

export default function AdminDeliveryPage() {
  return <AdminDelivery />;
}
