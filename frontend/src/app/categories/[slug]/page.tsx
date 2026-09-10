import type { Metadata } from "next";

import { ProductBrowser } from "@/components/catalog/product-browser";
import { SiteHeader } from "@/components/layout/site-header";

export const metadata: Metadata = {
  title: "Danh mục sản phẩm",
  description: "Khám phá nông sản theo danh mục tại GreenFarm"
};

export default async function CategoryPage({
  params
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  return (
    <div className="app-shell">
      <SiteHeader />
      <ProductBrowser initialCategory={slug} />
    </div>
  );
}
