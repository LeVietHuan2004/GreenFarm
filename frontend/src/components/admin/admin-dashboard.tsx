"use client";

import {
  ArrowRight,
  Boxes,
  CheckCircle2,
  CircleAlert,
  EyeOff,
  Download,
  TrendingUp,
  TicketPercent,
  PackageCheck,
  ShoppingBasket,
  Sparkles,
  Sprout,
  Tags,
  UsersRound
} from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { AdminShell } from "@/components/admin/admin-shell";
import { CatalogImage } from "@/components/catalog/catalog-image";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice, primaryProductImage, productStatusLabel } from "@/lib/catalog-format";
import { getAdminProducts } from "@/services/catalog-service";
import { downloadBusinessReport, getBusinessReport, type BusinessReport, type ReportRange } from "@/services/report-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Product } from "@/types/catalog";

type DashboardData = {
  products: number;
  visibleProducts: number;
  outOfStock: number;
  hiddenProducts: number;
  recentProducts: Product[];
};

const emptyDashboard: DashboardData = {
  products: 0,
  visibleProducts: 0,
  outOfStock: 0,
  hiddenProducts: 0,
  recentProducts: []
};

const dashboardModules = [
  { href: "/admin/users", label: "Người dùng", detail: "Tài khoản, vai trò và trạng thái truy cập", icon: UsersRound, tone: "mint", badge: "Quản lý" },
  { href: "/admin/categories", label: "Danh mục", detail: "Tổ chức các nhóm sản phẩm trên cửa hàng", icon: Tags, tone: "sky", badge: "Catalog" },
  { href: "/admin/products", label: "Sản phẩm", detail: "Giá bán, tồn kho, trạng thái và hình ảnh", icon: Boxes, tone: "peach", badge: "Catalog" },
  { href: "/products", label: "Cửa hàng", detail: "Kiểm tra catalog đang hiển thị cho khách", icon: ShoppingBasket, tone: "lavender", badge: "Xem nhanh" }
] as const;

const statusLabels: Record<string,string> = {
  pending:"Chờ xác nhận", processing:"Đang xử lý", ready_for_delivery:"Sẵn sàng giao",
  out_for_delivery:"Đang giao", delivered:"Đã giao", delivery_failed:"Giao thất bại",
  completed:"Hoàn tất", canceled:"Đã hủy"
};

function rangeForDays(days:number):ReportRange {
  const today = new Date();
  const first = new Date(today.getFullYear(),today.getMonth(),today.getDate() - days + 1);
  const date = (value:Date) => `${value.getFullYear()}-${String(value.getMonth()+1).padStart(2,"0")}-${String(value.getDate()).padStart(2,"0")}`;
  return {from:date(first),to:date(today)};
}

export function AdminDashboard() {
  const { user, token, hasHydrated } = useAuthStore();
  const [data, setData] = useState<DashboardData>(emptyDashboard);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [reportLoading, setReportLoading] = useState(true);
  const [catalogError, setCatalogError] = useState<string | null>(null);
  const [reportError, setReportError] = useState<string | null>(null);
  const [report, setReport] = useState<BusinessReport | null>(null);
  const [period, setPeriod] = useState<7|30|90>(30);
  const [exporting, setExporting] = useState(false);
  const range = useMemo(() => rangeForDays(period), [period]);

  useEffect(() => {
    if (!hasHydrated || !token || user?.role !== "admin") return;
    let active = true;
    Promise.all([
      getAdminProducts({ size: 6, sort: "updatedAt,desc" }),
      getAdminProducts({ status: "out_of_stock", size: 1 }),
      getAdminProducts({ status: "hidden", size: 1 })
    ])
      .then(([products, outOfStock, hidden]) => {
        if (!active) return;
        setData({
          products: products.totalElements,
          visibleProducts: Math.max(0, products.totalElements - hidden.totalElements),
          outOfStock: outOfStock.totalElements,
          hiddenProducts: hidden.totalElements,
          recentProducts: products.content
        });
      })
      .catch((requestError) => active && setCatalogError(getApiErrorMessage(requestError)))
      .finally(() => active && setCatalogLoading(false));
    return () => { active = false; };
  }, [hasHydrated, token, user?.role]);

  useEffect(() => {
    if (!hasHydrated || !token || user?.role !== "admin") return;
    let active = true;
    getBusinessReport(range).then(result => {if(active){setReport(result);setReportError(null);}})
      .catch(requestError => {if(active){setReport(null);setReportError(getApiErrorMessage(requestError));}})
      .finally(() => {if(active)setReportLoading(false);});
    return () => {active=false;};
  }, [hasHydrated, token, user?.role, range]);

  const changePeriod = (days:7|30|90) => {if(days===period)return;setReport(null);setReportError(null);setReportLoading(true);setPeriod(days);};
  const exportReport = async () => {
    setExporting(true);
    try {await downloadBusinessReport(range);} catch (requestError) {setReportError(getApiErrorMessage(requestError));}
    finally {setExporting(false);}
  };

  const visibilityRate = useMemo(() => data.products === 0
    ? 0
    : Math.round((data.visibleProducts / data.products) * 100), [data.products, data.visibleProducts]);

  const dailySeries = useMemo(() => {
    if (!report) return [];
    const revenue = new Map(report.dailyRevenue.map(day => [day.date,day.revenue]));
    const start = new Date(`${report.from}T00:00:00Z`);
    const end = new Date(`${report.to}T00:00:00Z`);
    const result:{date:string;revenue:number}[] = [];
    for (let date = start; date <= end; date = new Date(date.getTime()+86400000)) {
      const key = date.toISOString().slice(0,10);
      result.push({date:key,revenue:revenue.get(key) ?? 0});
    }
    return result;
  }, [report]);
  const maxDailyRevenue = Math.max(1,...dailySeries.map(day => day.revenue));
  const orderCount = report?.orderStatuses.reduce((sum,item) => sum + item.count,0) ?? 0;
  const couponUses = report?.couponPerformance.reduce((sum,item) => sum + item.uses,0) ?? 0;
  const couponDiscount = report?.couponPerformance.reduce((sum,item) => sum + item.discountAmount,0) ?? 0;

  const metrics = [
    { label: `Doanh thu ${period} ngày`, value: formatPrice(report?.netRevenue ?? 0), detail: `${report?.paidOrders ?? 0} đơn đã thanh toán`, icon: TrendingUp, tone: "amber", loading:reportLoading },
    { label: "Giá trị đơn trung bình", value: formatPrice(report?.averageOrderValue ?? 0), detail: "Không tính đơn hoàn tiền", icon: ShoppingBasket, tone: "rose", loading:reportLoading },
    { label: "Khách hàng mới", value: (report?.newCustomers ?? 0).toLocaleString("vi-VN"), detail: `Tài khoản khách trong ${period} ngày`, icon: UsersRound, tone: "green", loading:reportLoading },
    { label: "Tồn kho thấp", value: (report?.lowStockCount ?? 0).toLocaleString("vi-VN"), detail: `Còn 1–${report?.lowStockThreshold ?? 10} sản phẩm`, icon: CircleAlert, tone: "blue", loading:reportLoading }
  ];

  return (
    <AdminShell active="dashboard">
      <main className="admin-dashboard-main">
        <section className="admin-welcome">
          <div>
            <span className="admin-welcome-pill"><Sparkles size={14} /> Trung tâm vận hành</span>
            <h1>Xin chào, {user?.name?.split(" ").at(-1) ?? "Admin"}</h1>
            <p>Đây là tình hình catalog và tài khoản GreenFarm hôm nay.</p>
          </div>
          <Link href="/admin/products" className="admin-welcome-action"><Boxes size={17} /> Quản lý sản phẩm <ArrowRight size={16} /></Link>
        </section>

        {(catalogError || reportError) && <p className="catalog-notice error">{catalogError || reportError}</p>}

        <section className="admin-metric-grid" aria-label="Chỉ số tổng quan">
          {metrics.map(({ label, value, detail, icon: Icon, tone, loading }) => (
            <article className="admin-metric-card" key={label}>
              <div className="admin-metric-label"><span>{label}</span><i className={tone}><Icon size={18} /></i></div>
              <strong>{loading ? "—" : value}</strong>
              <small>{detail}</small>
            </article>
          ))}
        </section>

        <section className="admin-dashboard-section">
          <div className="admin-dashboard-section-heading admin-report-heading">
            <div><span className="eyebrow">{report?.from ?? range.from} — {report?.to ?? range.to}</span><h2>Báo cáo kinh doanh</h2></div>
            <div className="admin-report-controls">
              <div className="admin-report-periods" aria-label="Kỳ báo cáo">
                {([7,30,90] as const).map(days => <button key={days} type="button" aria-pressed={period===days} onClick={() => changePeriod(days)}>{days} ngày</button>)}
              </div>
              <button className="secondary-button" type="button" disabled={reportLoading || exporting || !report} onClick={() => void exportReport()}><Download size={16}/> {exporting ? "Đang xuất..." : "Xuất CSV"}</button>
            </div>
          </div>
          <div className="admin-report-grid">
            <article className="admin-report-card admin-revenue-card">
              <div className="admin-report-card-heading"><div><h3>Doanh thu theo ngày</h3><p>Thanh toán hoàn tất, không tính đơn đã hoàn tiền</p></div><strong>{reportLoading ? "—" : formatPrice(report?.netRevenue ?? 0)}</strong></div>
              {dailySeries.length > 0 ? <div className="admin-revenue-chart" role="list" aria-label="Doanh thu từng ngày">
                {dailySeries.map((day,index) => <div className="admin-revenue-day" role="listitem" key={day.date} title={`${day.date}: ${formatPrice(day.revenue)}`} aria-label={`${day.date}: ${formatPrice(day.revenue)}`}>
                  <span className="admin-revenue-track"><span style={{height:`${day.revenue===0?2:Math.max(4,day.revenue/maxDailyRevenue*100)}%`}}/></span>
                  <small>{index===0 || index===dailySeries.length-1 || index%Math.max(1,Math.floor(dailySeries.length/6))===0 ? day.date.slice(5) : ""}</small>
                </div>)}
              </div> : <div className="admin-empty">{reportLoading ? "Đang tải báo cáo..." : "Chưa có dữ liệu doanh thu."}</div>}
            </article>
            <article className="admin-report-card">
              <div className="admin-report-card-heading"><div><h3>Đơn hàng theo trạng thái</h3><p>Đơn được tạo trong kỳ · {orderCount.toLocaleString("vi-VN")} đơn</p></div><PackageCheck size={20}/></div>
              <div className="admin-status-list">{report?.orderStatuses.map(item => <div key={item.status}>
                <span>{statusLabels[item.status] ?? item.status}</span><strong>{item.count.toLocaleString("vi-VN")}</strong>
                <i><span style={{width:`${orderCount ? item.count/orderCount*100 : 0}%`}}/></i>
              </div>)}{!report && <div className="admin-empty">{reportLoading ? "Đang tải báo cáo..." : "Chưa có dữ liệu."}</div>}</div>
            </article>
            <article className="admin-report-card">
              <div className="admin-report-card-heading"><div><h3>Sản phẩm bán chạy</h3><p>Số lượng bán · doanh số trước ưu đãi</p></div><ShoppingBasket size={20}/></div>
              <div className="admin-report-list">{report?.topProducts.map((item,index) => <div key={item.productId}><span><b>{index+1}</b>{item.productName}</span><strong>{item.quantity.toLocaleString("vi-VN")} <small>· {formatPrice(item.revenue)}</small></strong></div>)}{report && report.topProducts.length===0 && <div className="admin-empty">Chưa có đơn thanh toán trong kỳ.</div>}{!report && reportLoading && <div className="admin-empty">Đang tải báo cáo...</div>}</div>
            </article>
            <article className="admin-report-card">
              <div className="admin-report-card-heading"><div><h3>Hiệu quả coupon</h3><p>{couponUses.toLocaleString("vi-VN")} lượt dùng · giảm {formatPrice(couponDiscount)} · 10 mã nhiều lượt nhất</p></div><TicketPercent size={20}/></div>
              <div className="admin-report-list">{report?.couponPerformance.slice(0,10).map(item => <div key={item.couponId}><span><b>{item.couponType==="FREESHIP" ? "🚚" : "%"}</b>{item.code}<small>{item.couponType==="FREESHIP" ? "Freeship" : "Giảm trên đơn"}</small></span><strong>{item.uses.toLocaleString("vi-VN")} lượt<small> · {formatPrice(item.discountAmount)}</small></strong></div>)}{report && report.couponPerformance.length===0 && <div className="admin-empty">Chưa có coupon trên đơn đã thanh toán.</div>}{!report && reportLoading && <div className="admin-empty">Đang tải báo cáo...</div>}</div>
            </article>
            <article className="admin-report-card">
              <div className="admin-report-card-heading"><div><h3>Tồn kho thấp</h3><p>Còn 1–{report?.lowStockThreshold ?? 10} · {report?.lowStockCount ?? 0} sản phẩm</p></div><CircleAlert size={20}/></div>
              <div className="admin-report-list">{report?.lowStockProducts.map(item => <div key={item.productId}><span>{item.productName}</span><strong>{item.stock} còn lại</strong></div>)}{report && report.lowStockProducts.length===0 && <div className="admin-empty">Không có sản phẩm tồn kho thấp.</div>}{!report && reportLoading && <div className="admin-empty">Đang tải báo cáo...</div>}</div>
              <Link className="admin-report-link" href="/admin/products">Quản lý tồn kho <ArrowRight size={14}/></Link>
            </article>
          </div>
        </section>

        <section className="admin-dashboard-section">
          <div className="admin-dashboard-section-heading">
            <div><span className="eyebrow">Truy cập nhanh</span><h2>Không gian quản lý</h2></div>
            <span>Các module đang hoạt động</span>
          </div>
          <div className="admin-dashboard-module-grid">
            {dashboardModules.map(({ href, label, detail, icon: Icon, tone, badge }) => (
              <Link href={href} className={`admin-dashboard-module ${tone}`} key={label}>
                <div className="admin-module-art" aria-hidden="true">
                  <span><Icon size={38} /></span><i /><i />
                </div>
                <span className="admin-module-badge">{badge}</span>
                <div><h3>{label}</h3><p>{detail}</p></div>
                <ArrowRight size={17} className="admin-module-arrow" />
              </Link>
            ))}
          </div>
        </section>

        <section className="admin-dashboard-bottom">
          <article className="admin-operations-card">
            <div className="admin-card-heading"><div><span className="eyebrow">Tình trạng</span><h2>Sức khỏe catalog</h2></div><PackageCheck size={21} /></div>
            <div className="admin-health-score"><strong>{catalogLoading ? "—" : `${visibilityRate}%`}</strong><span>Sản phẩm đang hiển thị công khai</span></div>
            <div className="admin-health-track"><span style={{ width: `${visibilityRate}%` }} /></div>
            <div className="admin-health-list">
              <div><span><CheckCircle2 size={16} /> Đang hiển thị</span><strong>{data.visibleProducts}</strong></div>
              <div><span><CircleAlert size={16} /> Hết hàng</span><strong>{data.outOfStock}</strong></div>
              <div><span><EyeOff size={16} /> Đang ẩn</span><strong>{data.hiddenProducts}</strong></div>
            </div>
            <Link href="/admin/products">Xem toàn bộ sản phẩm <ArrowRight size={15} /></Link>
          </article>

          <article className="admin-recent-card">
            <div className="admin-card-heading"><div><span className="eyebrow">Cập nhật gần đây</span><h2>Sản phẩm mới chỉnh sửa</h2></div><Sprout size={21} /></div>
            <div className="admin-recent-list">
              {data.recentProducts.slice(0, 5).map((product) => (
                <Link href="/admin/products" key={product.id}>
                  <span className="admin-recent-image"><CatalogImage src={primaryProductImage(product)} alt={product.name} /></span>
                  <span><strong>{product.name}</strong><small>{formatPrice(product.price)} · {productStatusLabel[product.status]}</small></span>
                  <ArrowRight size={15} />
                </Link>
              ))}
              {!catalogLoading && data.recentProducts.length === 0 && <div className="admin-empty">Chưa có sản phẩm.</div>}
            </div>
          </article>
        </section>
      </main>
    </AdminShell>
  );
}
