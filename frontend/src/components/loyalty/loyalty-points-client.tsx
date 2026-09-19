"use client";

import { CircleDollarSign, Coins, LoaderCircle } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { formatDateTime } from "@/lib/order-format";
import { getLoyaltySummary } from "@/services/loyalty-service";
import { useAuthStore } from "@/stores/auth-store";
import type { LoyaltySummary } from "@/types/loyalty";

const labels = { order_earn:"Điểm từ đơn hàng", review_earn:"Điểm từ đánh giá", redemption:"Đổi điểm thanh toán", redemption_restore:"Hoàn điểm đơn hủy" };

export function LoyaltyPointsClient({ embedded = false }: { embedded?: boolean }) {
  const { user, hasHydrated } = useAuthStore();
  const [summary, setSummary] = useState<LoyaltySummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { if (!hasHydrated || user?.role !== "customer") return; getLoyaltySummary().then(setSummary).catch((cause) => setError(getApiErrorMessage(cause))); }, [hasHydrated, user?.role]);

  const content = !hasHydrated || !summary && !error
    ? <p className="commerce-state"><LoaderCircle className="spin" size={17}/> Đang tải điểm tích lũy...</p>
    : user?.role !== "customer"
      ? <div className="catalog-empty"><Coins size={40}/><h2>Điểm tích lũy dành cho khách hàng</h2><Link className="primary-button" href="/login">Đăng nhập khách hàng</Link></div>
      : error
        ? <div className="catalog-empty"><h2>Không thể tải điểm tích lũy</h2><p>{error}</p></div>
        : <><section className="loyalty-balance-card"><span><Coins size={28}/></span><div><small>Điểm hiện có</small><strong>{summary!.pointsBalance.toLocaleString("vi-VN")} điểm</strong><p>1 điểm = {formatPrice(summary!.discountPerPoint)} · Tối đa dùng {summary!.maxRedemptionPercent}% mỗi đơn.</p></div><Link className="primary-button" href="/checkout"><CircleDollarSign size={17}/>Dùng điểm</Link></section><section className="loyalty-history"><h2>Lịch sử điểm</h2>{summary!.transactions.length===0?<div className="admin-empty">Chưa có giao dịch điểm. Điểm được cộng sau khi đơn giao thành công hoặc khi gửi đánh giá.</div>:summary!.transactions.map(transaction=><article key={transaction.id}><div><strong>{labels[transaction.type]??transaction.type}</strong><small>{transaction.description} · {formatDateTime(transaction.createdAt)}</small></div><b className={transaction.points>0?"earn":"spend"}>{transaction.points>0?"+":""}{transaction.points.toLocaleString("vi-VN")} điểm</b></article>)}</section></>;

  if (embedded) return <section className="profile-main-card profile-tab-card"><header><div><h2>Điểm GreenFarm</h2><p>Tích điểm, đổi điểm và xem lịch sử giao dịch.</p></div><Coins size={20}/></header>{content}</section>;
  return <main className="catalog-main loyalty-main"><header className="commerce-heading"><span className="eyebrow">GreenFarm Rewards</span><h1>Điểm tích lũy</h1><p>Mua hàng và đánh giá sản phẩm để nhận điểm, sau đó dùng điểm giảm giá ở bước thanh toán.</p></header>{content}</main>;
}
