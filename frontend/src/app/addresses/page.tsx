import type { Metadata } from "next";
import { AddressBook } from "@/components/order/address-book";
import { SiteHeader } from "@/components/layout/site-header";
export const metadata:Metadata={title:"Địa chỉ giao hàng"};
export default function AddressesPage(){return <div className="app-shell"><SiteHeader/><main className="catalog-main"><header className="commerce-heading"><span className="eyebrow">Tài khoản GreenFarm</span><h1>Sổ địa chỉ</h1><p>Quản lý thông tin nhận hàng của bạn.</p></header><AddressBook/></main></div>}
