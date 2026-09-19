"use client";

import { ArrowLeft, Download, Printer, ReceiptText } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { formatDateTime, orderStatusLabel } from "@/lib/order-format";
import { orderService } from "@/services/order-service";
import type { Order } from "@/types/order";

export function InvoiceClient({ id }: { id: number }) {
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    orderService.findOne(id).then((data) => active && setOrder(data)).catch((cause) => active && setError(getApiErrorMessage(cause)));
    return () => { active = false; };
  }, [id]);

  if (error) return <main className="catalog-main"><div className="catalog-empty"><ReceiptText size={40}/><h1>Không thể mở hóa đơn</h1><p>{error}</p><Link className="primary-button" href="/orders">Đơn hàng của tôi</Link></div></main>;
  if (!order) return <main className="catalog-main"><p className="commerce-state">Đang tạo hóa đơn...</p></main>;

  const paymentLabel = order.payment?.method === "cod" ? "Thanh toán khi nhận hàng (COD)" : "Thanh toán VNPAY";
  const paymentStatus = order.payment?.status === "completed" ? "Đã thanh toán" : order.payment?.status === "refunded" ? "Đã hoàn tiền" : order.payment?.status === "failed" ? "Không thành công" : "Chờ thanh toán";

  return <main className="catalog-main invoice-page-main">
    <div className="invoice-page-actions no-print"><Link className="commerce-back" href={`/orders/${order.id}`}><ArrowLeft size={16}/>Chi tiết đơn hàng</Link><button className="secondary-button" type="button" onClick={() => window.print()}><Printer size={16}/>In / Lưu PDF</button></div>
    <article className="invoice-sheet">
      <header className="invoice-header"><div><span className="invoice-brand">GreenFarm</span><p>Nông sản tươi mỗi ngày</p></div><div><span className="eyebrow">Hóa đơn mua hàng</span><h1>#{order.id}</h1><small>Ngày tạo: {formatDateTime(order.createdAt)}</small></div></header>
      <section className="invoice-party-grid"><div><h2>Khách hàng</h2><strong>{order.recipientName}</strong><p>{order.recipientPhone}<br />{order.shippingAddress}, {order.shippingCity}</p></div><div><h2>Trạng thái</h2><p><span className={`order-status ${order.status}`}>{orderStatusLabel[order.status] ?? order.status}</span></p><strong>{paymentStatus}</strong><p>{paymentLabel}</p>{order.payment?.referenceCode && <small>Mã thanh toán: {order.payment.referenceCode}</small>}</div></section>
      <section className="invoice-items"><div className="invoice-table-head"><span>Sản phẩm</span><span>SL</span><span>Đơn giá</span><span>Thành tiền</span></div>{order.items.map((item) => <div className="invoice-item" key={item.id}><span><strong>{item.productName}</strong><small>{item.productUnit || "Sản phẩm"}</small></span><span>{item.quantity}</span><span>{formatPrice(item.unitPrice)}</span><strong>{formatPrice(item.lineTotal)}</strong></div>)}</section>
      <section className="invoice-footer"><p>Hóa đơn điện tử được tạo tự động từ GreenFarm. Cảm ơn bạn đã mua sắm!</p><div className="invoice-totals"><div><span>Tạm tính</span><strong>{formatPrice(order.subtotal)}</strong></div>{order.discountAmount > 0 && <div className="discount-row"><span>Giảm giá {order.couponCode && `(${order.couponCode})`}</span><strong>−{formatPrice(order.discountAmount)}</strong></div>}{order.loyaltyDiscountAmount > 0 && <div className="discount-row"><span>Điểm tích lũy ({order.loyaltyPointsUsed} điểm)</span><strong>−{formatPrice(order.loyaltyDiscountAmount)}</strong></div>}<div><span>Phí giao hàng</span><strong>{formatPrice(order.shippingFee)}</strong></div>{order.shippingDiscountAmount > 0 && <div className="discount-row"><span>Giảm phí vận chuyển {order.shippingCouponCode && `(${order.shippingCouponCode})`}</span><strong>−{formatPrice(order.shippingDiscountAmount)}</strong></div>}<div className="invoice-grand-total"><span>Tổng cộng</span><strong>{formatPrice(order.total)}</strong></div></div></section>
    </article>
    <p className="invoice-email-note no-print"><Download size={15}/> Hóa đơn đã được gửi đến email đăng ký của bạn.</p>
  </main>;
}
