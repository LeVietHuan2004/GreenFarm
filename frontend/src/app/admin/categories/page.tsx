import type { Metadata } from "next";

import { AdminCategories } from "@/components/catalog/admin-categories";

export const metadata: Metadata = { title: "Quản lý danh mục" };

export default function AdminCategoriesPage() {
  return <AdminCategories />;
}
