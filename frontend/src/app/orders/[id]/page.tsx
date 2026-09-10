import type { Metadata } from "next";
import { SiteHeader } from "@/components/layout/site-header";
import { OrderDetailClient } from "@/components/order/order-detail-client";
export const metadata:Metadata={title:"Chi tiết đơn hàng"};
export default async function OrderDetailPage({params}:{params:Promise<{id:string}>}){const {id}=await params;return <div className="app-shell"><SiteHeader/><OrderDetailClient id={Number(id)}/></div>}
