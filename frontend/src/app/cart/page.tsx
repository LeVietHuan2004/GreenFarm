import type { Metadata } from "next";
import { CommercePage } from "@/components/commerce/commerce-page";
import { SiteHeader } from "@/components/layout/site-header";

export const metadata: Metadata = { title: "Giỏ hàng" };

export default function CartPage() {
  return <div className="app-shell"><SiteHeader /><CommercePage kind="cart" /></div>;
}
