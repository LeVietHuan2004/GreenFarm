import type { Metadata } from "next";
import { SiteHeader } from "@/components/layout/site-header";
import { DeliveryTrackingClient } from "@/components/order/delivery-tracking-client";

export const metadata: Metadata = { title: "Theo dõi giao hàng" };

export default function ShippingPage() {
  return <div className="app-shell"><SiteHeader /><DeliveryTrackingClient /></div>;
}
