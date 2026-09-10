import type { Metadata } from "next";
import { Suspense } from "react";
import { PaymentResultClient } from "@/components/order/payment-result-client";
import { SiteHeader } from "@/components/layout/site-header";

export const metadata: Metadata = { title: "Kết quả thanh toán" };

export default function PaymentResultPage(){
  return <div className="app-shell"><SiteHeader/><Suspense fallback={<main className="catalog-main"><p className="commerce-state">Đang kiểm tra kết quả thanh toán...</p></main>}><PaymentResultClient/></Suspense></div>;
}
