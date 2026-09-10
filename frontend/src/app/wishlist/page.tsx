import type { Metadata } from "next";
import { CommercePage } from "@/components/commerce/commerce-page";
import { SiteHeader } from "@/components/layout/site-header";

export const metadata: Metadata = { title: "Yêu thích" };

export default function WishlistPage() {
  return <div className="app-shell"><SiteHeader /><CommercePage kind="wishlist" /></div>;
}
