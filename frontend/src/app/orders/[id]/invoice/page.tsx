import type { Metadata } from "next";
import { SiteHeader } from "@/components/layout/site-header";
import { InvoiceClient } from "@/components/order/invoice-client";

export const metadata: Metadata = { title: "Hóa đơn" };

export default async function InvoicePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <div className="app-shell"><SiteHeader /><InvoiceClient id={Number(id)} /></div>;
}
