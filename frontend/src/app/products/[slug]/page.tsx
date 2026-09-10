import type { Metadata } from "next";

import { ProductDetail } from "@/components/catalog/product-detail";
import { SiteHeader } from "@/components/layout/site-header";

export const metadata: Metadata = {
  title: "Chi tiết sản phẩm",
  description: "Thông tin sản phẩm, giá và tồn kho tại GreenFarm"
};

export default async function ProductDetailPage({
  params
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  return (
    <div className="app-shell">
      <SiteHeader />
      <ProductDetail slug={slug} />
    </div>
  );
}
