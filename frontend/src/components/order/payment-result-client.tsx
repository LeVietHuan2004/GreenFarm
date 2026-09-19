"use client";

import { CheckCircle2, CircleX, ReceiptText } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";

export function PaymentResultClient(){
  const params=useSearchParams(); const success=params?.get("success")==="true"; const message=params?.get("message"); const orderId=params?.get("orderId");
  const orderLink=orderId?(success?`/orders/${orderId}/invoice`:`/orders/${orderId}`):"/orders";
  return <main className="catalog-main payment-result-main"><section className={`payment-result ${success?"success":"failed"}`}>{success?<CheckCircle2 size={48}/>:<CircleX size={48}/>}<span className="eyebrow">Kết quả thanh toán</span><h1>{success?"Thanh toán thành công":"Thanh toán chưa hoàn tất"}</h1><p>{message??(success?"VNPAY đã xác nhận giao dịch của bạn.":"Không thể xác nhận giao dịch. Bạn có thể kiểm tra lại đơn hàng.")}</p>{orderId&&<p className="payment-order-reference"><ReceiptText size={16}/>Đơn hàng #{orderId}</p>}<div><Link className="primary-button" href={orderLink}>{success?"Xem hóa đơn":"Xem đơn hàng"}</Link><Link className="secondary-button" href="/">Về cửa hàng</Link></div></section></main>;
}
