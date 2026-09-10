import type { Metadata } from "next";
import { SiteHeader } from "@/components/layout/site-header";
import { OrdersClient } from "@/components/order/orders-client";
export const metadata:Metadata={title:"Đơn hàng"};
export default function OrdersPage(){return <div className="app-shell"><SiteHeader/><OrdersClient/></div>}
