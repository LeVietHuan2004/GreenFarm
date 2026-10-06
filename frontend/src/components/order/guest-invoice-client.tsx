"use client";

import { ArrowLeft, ReceiptText } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { InvoiceDocument } from "@/components/order/invoice-client";
import { getApiErrorMessage } from "@/lib/api-error";
import { getGuestOrders, saveGuestOrder } from "@/lib/guest-session";
import { guestCommerceService } from "@/services/guest-commerce-service";
import type { Order } from "@/types/order";

export function GuestInvoiceClient({ id }: { id: number }) {
  const [email, setEmail] = useState("");
  const [token, setToken] = useState("");
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.resolve().then(async () => {
      const stored = getGuestOrders().find((item) => item.orderId === id);
      if (!active || !stored) return;
      setEmail(stored.email);
      setToken(stored.token);
      setLoading(true);
      try {
        const current = await guestCommerceService.lookup(id, stored.email, stored.token);
        if (active) setOrder(current);
      } catch (cause) {
        if (active) setError(getApiErrorMessage(cause));
      } finally {
        if (active) setLoading(false);
      }
    });
    return () => { active = false; };
  }, [id]);

  async function verify() {
    if (!Number.isInteger(id) || id < 1 || !email.trim() || !token.trim()) {
      setError("Nhập email và mã xác minh để mở hóa đơn.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const current = await guestCommerceService.lookup(id, email.trim(), token.trim());
      saveGuestOrder({ orderId: id, email: email.trim().toLowerCase(), token: token.trim() });
      setOrder(current);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setLoading(false);
    }
  }

  if (order) return <InvoiceDocument order={order} backHref={`/guest-orders?orderId=${id}`} emailNote="Hóa đơn đã được gửi đến email đặt hàng của bạn." />;

  return <main className="catalog-main invoice-page-main">
    <Link className="commerce-back" href={`/guest-orders?orderId=${id}`}><ArrowLeft size={16}/>Tra cứu đơn hàng</Link>
    <section className="guest-order-lookup guest-invoice-verification">
      <div className="guest-invoice-intro"><ReceiptText size={24}/><div><h1>Hóa đơn đơn hàng #{id}</h1><p>Xác minh bằng email đặt hàng và mã tra cứu để xem hoặc in hóa đơn.</p></div></div>
      <form onSubmit={(event) => { event.preventDefault(); void verify(); }}>
        <label>Email đặt hàng<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required /></label>
        <label>Mã xác minh đơn<input type="password" value={token} onChange={(event) => setToken(event.target.value)} autoComplete="off" required /></label>
        {error && <p role="alert" className="guest-invoice-error">{error}</p>}
        <button className="primary-button" type="submit" disabled={loading}>{loading ? "Đang xác minh..." : "Mở hóa đơn"}</button>
      </form>
    </section>
  </main>;
}
