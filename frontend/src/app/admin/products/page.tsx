import type { Metadata } from "next";

import { AdminProducts } from "@/components/catalog/admin-products";

export const metadata: Metadata = { title: "Quản lý sản phẩm" };

export default async function AdminProductsPage({
  searchParams
}: {
  searchParams: Promise<{ search?: string | string[] }>;
}) {
  const query = await searchParams;
  const search = typeof query.search === "string" ? query.search : "";
  return <AdminProducts initialSearch={search} />;
}
