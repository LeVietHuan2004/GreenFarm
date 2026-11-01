import type { Metadata } from "next";
import { AdminOrderDetailPage } from "@/components/admin/admin-order-detail-page";

export const metadata: Metadata = { title: "Chi tiết đơn hàng" };

export default async function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <AdminOrderDetailPage orderId={id} />;
}
