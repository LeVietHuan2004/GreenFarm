"use client";

import { Search, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { formatPrice } from "@/lib/catalog-format";
import { getApiErrorMessage } from "@/lib/api-error";
import { getGuestOrders } from "@/lib/guest-session";
import { guestCommerceService } from "@/services/guest-commerce-service";
import type { Order } from "@/types/order";

export function GuestOrderLookup(){
  const params=useSearchParams(); const initialId=Number(params?.get("orderId"))||0;
  const [orderId,setOrderId]=useState(initialId?String(initialId):""); const [email,setEmail]=useState(""); const [token,setToken]=useState("");
  const [order,setOrder]=useState<Order|null>(null); const [busy,setBusy]=useState(false);
  useEffect(()=>{
    let active=true;
    Promise.resolve().then(async()=>{
      const stored=getGuestOrders().find(item=>item.orderId===initialId);
      if(!active||!stored)return;
      setEmail(stored.email);setToken(stored.token);setBusy(true);
      try{const current=await guestCommerceService.lookup(stored.orderId,stored.email,stored.token);if(active)setOrder(current);}
      catch(error){if(active)toast.error(getApiErrorMessage(error));}
      finally{if(active)setBusy(false);}
    });
    return ()=>{active=false;};
  },[initialId]);
  const lookup=async()=>{const id=Number(orderId);if(!Number.isInteger(id)||id<1||!email||!token){toast.error("Nhập mã đơn, email và mã xác minh");return;}setBusy(true);try{setOrder(await guestCommerceService.lookup(id,email.trim(),token.trim()));}catch(error){setOrder(null);toast.error(getApiErrorMessage(error));}finally{setBusy(false);}};
  return <main className="catalog-main guest-order-main"><header className="commerce-heading"><span className="eyebrow">Đơn hàng khách</span><h1>Tra cứu đơn hàng</h1><p>Nhập đủ ba thông tin để bảo vệ đơn hàng và thông tin nhận hàng của bạn.</p></header>
    <form className="guest-order-lookup" onSubmit={e=>{e.preventDefault();void lookup();}}><label>Mã đơn hàng<input inputMode="numeric" value={orderId} onChange={e=>setOrderId(e.target.value)} placeholder="Ví dụ: 42"/></label><label>Email đặt hàng<input type="email" value={email} onChange={e=>setEmail(e.target.value)} placeholder="ban@example.com"/></label><label>Mã xác minh đơn<input type="password" value={token} onChange={e=>setToken(e.target.value)} placeholder="Mã được lưu sau khi đặt hàng"/></label><button className="primary-button" disabled={busy}><Search size={17}/>{busy?"Đang xác minh...":"Tra cứu"}</button><small><ShieldCheck size={15}/>Mã xác minh được lưu trên thiết bị này. Không chia sẻ mã cho người khác.</small></form>
    {order&&<section className="guest-order-result"><header><div><span className="eyebrow">Đơn hàng #{order.id}</span><h2>{order.recipientName}</h2><p>{order.shippingAddress}, {order.shippingCity}</p></div><strong>{order.status}</strong></header><div className="guest-order-items">{order.items.map(item=><article key={item.id}><span>{item.productName}<small>{item.quantity} × {formatPrice(item.unitPrice)}</small></span><strong>{formatPrice(item.lineTotal)}</strong></article>)}</div><footer><span>Tổng thanh toán</span><strong>{formatPrice(order.total)}</strong></footer>{order.payment&&<p>Thanh toán: <b>{order.payment.method.toUpperCase()}</b> · {order.payment.status}</p>}<Link className="secondary-button" href={`/guest-orders/${order.id}/invoice`}>Xem hóa đơn</Link></section>}
  </main>;
}
