import type { Metadata } from "next";
import { SiteHeader } from "@/components/layout/site-header";
import { GuestInvoiceClient } from "@/components/order/guest-invoice-client";

export const metadata: Metadata = { title: "Hóa đơn đơn hàng khách" };

export default async function GuestInvoicePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <div className="app-shell"><SiteHeader /><GuestInvoiceClient id={Number(id)} /></div>;
}
