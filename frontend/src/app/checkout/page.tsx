import type { Metadata } from "next";
import { CheckoutClient } from "@/components/order/checkout-client";
import { SiteHeader } from "@/components/layout/site-header";
export const metadata:Metadata={title:"Thanh toán"};
export default function CheckoutPage(){return <div className="app-shell"><SiteHeader/><CheckoutClient/></div>}
