import type { Metadata } from "next";

import { AdminCoupons } from "@/components/admin/admin-coupons";

export const metadata: Metadata = { title: "Quản lý mã giảm giá" };

export default function AdminCouponsPage() {
  return <AdminCoupons />;
}
