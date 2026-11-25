"use client";

import { ArrowLeft, ArrowRight, CircleAlert, Clock3, MapPin, PackageCheck, PackageSearch, RefreshCw, Search, Truck } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { deliveryFilters, deliveryMessage, matchesDeliveryFilter, type DeliveryFilter } from "@/lib/delivery-tracking";
import { getGuestOrders } from "@/lib/guest-session";
import { formatDateTime, orderStatusLabel } from "@/lib/order-format";
import { guestCommerceService } from "@/services/guest-commerce-service";
import { orderService } from "@/services/order-service";
import { useAuthStore } from "@/stores/auth-store";
import type { OrderSummary } from "@/types/order";
import { DeliveryProgress } from "./delivery-progress";
import styles from "./delivery-tracking.module.css";

export function DeliveryTrackingClient() {
  const { hasHydrated, user } = useAuthStore();
  if (!hasHydrated) return <main className="catalog-main"><p className="commerce-state" role="status">Đang tải đơn giao hàng...</p></main>;
  return <DeliveryTrackingSession key={user ? `${user.role}:${user.id}` : "guest"} />;
}

function DeliveryTrackingSession() {
  const user = useAuthStore((state) => state.user);
  const [items, setItems] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  const [filter, setFilter] = useState<DeliveryFilter>("active");
  const [search, setSearch] = useState("");
  const refresh = useRef<() => void>(() => undefined);
  const customer = user?.role === "customer";
  const allowed = !user || customer;

  useEffect(() => {
    if (!allowed) return;
    let active = true;
    let inFlight = false;

    const load = async () => {
      if (inFlight || !active) return;
      inFlight = true;
      setRefreshing(true);
      try {
        let next: OrderSummary[];
        let notice: string | null = null;
        if (customer) {
          next = await orderService.findAll();
        } else {
          const stored = getGuestOrders().filter((item) => Number.isInteger(item?.orderId) && item.orderId > 0
            && typeof item.email === "string" && typeof item.token === "string").slice(0, 10);
          const results = await Promise.allSettled(stored.map((item) => guestCommerceService.lookup(item.orderId, item.email, item.token)));
          next = results.flatMap((result) => {
            if (result.status !== "fulfilled") return [];
            const order = result.value;
            return [{ ...order, itemCount: order.items.reduce((count, item) => count + item.quantity, 0) }];
          });
          const failures = results.filter((result) => result.status === "rejected").length;
          if (failures) notice = `Chưa tải được ${failures} đơn đã lưu. Bạn có thể thử cập nhật lại hoặc tra cứu bằng mã đơn.`;
        }
        if (!active) return;
        setItems([...next].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
        setError(notice);
        setUpdatedAt(new Date());
      } catch (reason) {
        if (active) setError(getApiErrorMessage(reason));
      } finally {
        inFlight = false;
        if (active) { setLoading(false); setRefreshing(false); }
      }
    };

    refresh.current = () => { void load(); };
    void Promise.resolve().then(load);
    const onVisible = () => { if (document.visibilityState === "visible") void load(); };
    const timer = window.setInterval(onVisible, 30_000);
    window.addEventListener("focus", onVisible);
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      active = false;
      refresh.current = () => undefined;
      window.clearInterval(timer);
      window.removeEventListener("focus", onVisible);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [allowed, customer]);

  const query = search.trim().replace(/^#/, "").toLocaleLowerCase("vi-VN");
  const visible = items.filter((item) => matchesDeliveryFilter(item.status, filter)
    && (!query || String(item.id).includes(query) || item.shippingCity.toLocaleLowerCase("vi-VN").includes(query)));
  const counts = {
    waiting: items.filter((item) => ["pending", "processing", "ready_for_delivery"].includes(item.status)).length,
    shipping: items.filter((item) => ["out_for_delivery", "delivery_failed"].includes(item.status)).length,
    delivered: items.filter((item) => ["delivered", "completed"].includes(item.status)).length,
  };

  return <main className={`catalog-main ${styles.main}`}>
    <Link className="commerce-back" href="/"><ArrowLeft size={16} />Về cửa hàng</Link>
    <header className={styles.heading}>
      <div><span className="eyebrow">Hành trình đơn hàng</span><h1>Theo dõi giao hàng</h1><p>Từ lúc đặt đơn đến khi nhận hàng, mọi cập nhật đều ở đây.</p></div>
      <span className={styles.headingIcon} aria-hidden="true"><Truck size={36} strokeWidth={1.5} /></span>
    </header>

    {!allowed ? <section className={styles.empty}><PackageSearch size={40} /><h2>Xem đơn bằng tài khoản khách hàng</h2><p>Đăng nhập tài khoản đã dùng để mua sắm hoặc tra cứu đơn hàng khách.</p><Link className="primary-button" href="/login">Đăng nhập khách hàng</Link><Link className="text-link" href="/guest-orders">Tra cứu đơn hàng khách</Link></section> : <>
      {!customer && <div className={styles.guestNotice}><PackageSearch size={21} aria-hidden="true" /><p>Đơn mua không đăng nhập được hiển thị từ thông tin đã lưu trên thiết bị này.</p><Link href="/guest-orders">Tra cứu đơn khác <ArrowRight size={15} /></Link><Link href="/login">Đăng nhập</Link></div>}

      <section className={styles.summary} aria-label="Tổng quan giao hàng">
        <div><span><Clock3 size={22} aria-hidden="true" /></span><p>Chờ xác nhận / giao hàng<strong>{loading ? "—" : counts.waiting}</strong></p></div>
        <div><span><Truck size={22} aria-hidden="true" /></span><p>Đang giao / cần giao lại<strong>{loading ? "—" : counts.shipping}</strong></p></div>
        <div><span><PackageCheck size={22} aria-hidden="true" /></span><p>Đã giao thành công<strong>{loading ? "—" : counts.delivered}</strong></p></div>
      </section>

      <section className={styles.workspace} aria-label="Đơn giao hàng của bạn" aria-busy={refreshing}>
        <div className={styles.toolbar}><h2>Đơn giao của bạn</h2><div><small>{updatedAt ? `Cập nhật ${updatedAt.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}` : ""}</small><button type="button" className={styles.refresh} onClick={() => refresh.current()} disabled={refreshing}><RefreshCw size={15} className={refreshing ? styles.spinning : undefined} aria-hidden="true" />{refreshing ? "Đang cập nhật" : "Cập nhật"}</button></div></div>
        <div className={styles.filters} role="group" aria-label="Lọc trạng thái giao hàng">
          {deliveryFilters.map((tab) => <button type="button" key={tab.id} aria-pressed={filter === tab.id} onClick={() => setFilter(tab.id)}>{tab.label}<span>{items.filter((item) => matchesDeliveryFilter(item.status, tab.id)).length}</span></button>)}
        </div>
        <label className={styles.search}><Search size={18} aria-hidden="true" /><input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tìm theo mã đơn hoặc tỉnh, thành nhận hàng" aria-label="Tìm đơn giao hàng" /></label>

        {error && <div className={styles.error} role="alert"><CircleAlert size={18} aria-hidden="true" /><p>{error}</p><button type="button" disabled={refreshing} onClick={() => refresh.current()}>Thử lại</button></div>}
        {loading ? <p className={styles.loading} role="status">Đang tải đơn giao hàng...</p> : visible.length ? <div className={styles.orders}>
          {visible.map((item) => <article className={styles.card} key={item.id}>
            <header><div><h3>Đơn hàng #{item.id}</h3><small>Đặt lúc {formatDateTime(item.createdAt)}</small></div><span className={`order-status ${item.status}`}>{orderStatusLabel[item.status] ?? item.status}</span></header>
            <div className={`${styles.statusMessage} ${item.status === "delivery_failed" ? styles.warning : ""}`}><Truck size={19} aria-hidden="true" /><p>{deliveryMessage(item.status)}</p></div>
            <DeliveryProgress status={item.status} />
            <div className={styles.destination}><MapPin size={17} aria-hidden="true" /><span>Giao đến <strong>{item.shippingCity}</strong></span><small>{item.shippingProvider === "ghn" ? "Giao hàng nhanh (GHN)" : "GreenFarm giao hàng"}</small></div>
            <footer><p>{item.itemCount} sản phẩm <span>·</span> <strong>{formatPrice(item.total)}</strong></p><Link href={customer ? `/orders/${item.id}` : `/guest-orders?orderId=${item.id}`}>Theo dõi chi tiết <ArrowRight size={16} aria-hidden="true" /></Link></footer>
          </article>)}
        </div> : !error && <div className={styles.empty}><PackageSearch size={44} strokeWidth={1.5} aria-hidden="true" /><h3>{query ? "Không tìm thấy đơn phù hợp" : items.length ? "Không có đơn ở trạng thái này" : "Bạn chưa có đơn giao hàng"}</h3><p>{query ? "Thử tìm bằng mã đơn hoặc tỉnh, thành nhận hàng khác." : items.length ? "Chọn trạng thái khác để xem các đơn hàng của bạn." : "Các đơn đã đặt sẽ xuất hiện tại đây để bạn theo dõi."}</p>{items.length ? <button type="button" className="secondary-button" onClick={() => { setFilter("all"); setSearch(""); }}>Xem tất cả đơn</button> : <Link className="primary-button" href="/products">Khám phá sản phẩm <ArrowRight size={16} /></Link>}</div>}
      </section>
      <p className={styles.help}>Cần hỗ trợ về đơn giao hàng? <Link href="/contact">Liên hệ GreenFarm <ArrowRight size={14} /></Link></p>
    </>}
  </main>;
}
