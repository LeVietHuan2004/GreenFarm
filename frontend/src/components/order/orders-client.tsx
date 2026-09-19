"use client";

import { ArrowLeft, Box, ChevronRight, PackageSearch } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { formatDateTime, orderStatusLabel } from "@/lib/order-format";
import { orderService } from "@/services/order-service";
import { useAuthStore } from "@/stores/auth-store";
import type { OrderSummary } from "@/types/order";

export function OrdersClient({ embedded = false }: { embedded?: boolean }) {
  const { hasHydrated, user } = useAuthStore();
  const [items, setItems] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!hasHydrated || !user || user.role !== "customer") return;
    let active = true;
    orderService.findAll().then((data) => active && setItems(data)).catch((reason) => active && setError(getApiErrorMessage(reason))).finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [hasHydrated, user]);

  const content = !hasHydrated || loading
    ? <p className="commerce-state">Đang tải đơn hàng...</p>
    : !user || user.role !== "customer"
      ? <div className="catalog-empty"><PackageSearch size={40}/><h2>Đăng nhập khách hàng để xem đơn hàng</h2><Link className="primary-button" href="/login">Đăng nhập</Link></div>
      : error
        ? <div className="catalog-empty"><h2>Không thể tải đơn hàng</h2><p>{error}</p></div>
        : items.length === 0
          ? <div className="catalog-empty"><Box size={42}/><h2>Bạn chưa có đơn hàng</h2><Link className="primary-button" href="/products">Bắt đầu mua sắm</Link></div>
          : <section className="order-list">{items.map((item) => <Link href={`/orders/${item.id}`} className="order-list-item" key={item.id}><div><span className={`order-status ${item.status}`}>{orderStatusLabel[item.status] ?? item.status}</span><h2>Đơn hàng #{item.id}</h2><p>{formatDateTime(item.createdAt)} · {item.itemCount} sản phẩm</p></div><div><strong>{formatPrice(item.total)}</strong><small>Giao tới {item.shippingCity}</small></div><ChevronRight/></Link>)}</section>;

  if (embedded) return <section className="profile-main-card profile-tab-card"><header><div><h2>Đơn mua của tôi</h2><p>Theo dõi tất cả đơn hàng theo thời gian.</p></div><Box size={20}/></header>{content}</section>;
  return <main className="catalog-main orders-main"><Link className="commerce-back" href="/"><ArrowLeft size={16}/>Về cửa hàng</Link><header className="commerce-heading"><span className="eyebrow">Tài khoản GreenFarm</span><h1>Đơn hàng của bạn</h1><p>Theo dõi tất cả đơn hàng theo thời gian.</p></header>{content}</main>;
}
