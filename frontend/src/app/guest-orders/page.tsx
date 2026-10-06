import { Suspense } from "react";
import { GuestOrderLookup } from "@/components/order/guest-order-lookup";
import { SiteHeader } from "@/components/layout/site-header";
export default function GuestOrdersPage(){return <div className="app-shell"><SiteHeader/><Suspense fallback={<main className="catalog-main">Đang tải...</main>}><GuestOrderLookup/></Suspense></div>;}
