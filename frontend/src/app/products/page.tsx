import type { Metadata } from "next";

import { ProductBrowser } from "@/components/catalog/product-browser";
import { SiteHeader } from "@/components/layout/site-header";

export const metadata: Metadata = {
  title: "Sản phẩm",
  description: "Tìm kiếm và lọc nông sản tươi tại GreenFarm"
};

export default async function ProductsPage({
  searchParams
}: {
  searchParams: Promise<{ search?: string | string[]; category?: string | string[] }>;
}) {
  const query = await searchParams;
  const search = typeof query.search === "string" ? query.search : "";
  const category = typeof query.category === "string" ? query.category : "";

  return (
    <div className="app-shell">
      <SiteHeader />
      <ProductBrowser initialSearch={search} initialCategory={category} />
    </div>
  );
}
