"use client";

import { ArrowUpRight, CreditCard, Leaf, Mail, MapPin, MessageCircle, Phone } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

const storePhone = process.env.NEXT_PUBLIC_STORE_PHONE?.trim();
const storeEmail = process.env.NEXT_PUBLIC_STORE_EMAIL?.trim();
const storeAddress = process.env.NEXT_PUBLIC_STORE_ADDRESS?.trim();
const phoneHref = storePhone?.replace(/[^\d+]/g, "");

export function SiteFooter() {
  const pathname = usePathname();
  const privateSections = ["/admin", "/staff", "/delivery"];
  const authPages = ["/login", "/register", "/forgot-password", "/reset-password"];
  if (!pathname || privateSections.some(path => pathname === path || pathname.startsWith(`${path}/`)) || authPages.includes(pathname) || pathname.endsWith("/invoice")) return null;

  return <footer className="site-footer" aria-label="Thông tin GreenFarm">
    <div className="site-footer-inner">
      <div className="site-footer-main">
        <div className="site-footer-brand">
          <Link href="/" className="site-footer-logo" aria-label="GreenFarm - về trang chủ"><span><Leaf size={22} aria-hidden="true" /></span><strong>GreenFarm</strong></Link>
          <p>Nông sản tươi cho bữa ăn mỗi ngày. Khám phá sản phẩm và lựa chọn phù hợp cho gia đình bạn.</p>
          <Link className="site-footer-discover" href="/products">Khám phá cửa hàng <ArrowUpRight size={16} aria-hidden="true" /></Link>
        </div>

        <nav className="site-footer-column" aria-label="Khám phá GreenFarm">
          <h2>Khám phá</h2>
          <Link href="/">Trang chủ</Link>
          <Link href="/#categories">Danh mục sản phẩm</Link>
          <Link href="/products">Tất cả sản phẩm</Link>
          <Link href="/wishlist">Sản phẩm yêu thích</Link>
        </nav>

        <nav className="site-footer-column" aria-label="Hỗ trợ mua hàng">
          <h2>Hỗ trợ mua hàng</h2>
          <Link href="/cart">Giỏ hàng</Link>
          <Link href="/orders">Đơn hàng của tôi</Link>
          <Link href="/guest-orders">Tra cứu đơn hàng</Link>
          <Link href="/contact">Liên hệ hỗ trợ</Link>
        </nav>

        <div className="site-footer-contact">
          <h2>Kết nối với GreenFarm</h2>
          <p>Bạn cần hỗ trợ về sản phẩm, đơn hàng hoặc giao nhận?</p>
          <Link href="/contact" className="site-footer-contact-link"><MessageCircle size={17} aria-hidden="true" /> Gửi yêu cầu hỗ trợ <ArrowUpRight size={15} aria-hidden="true" /></Link>
          {storePhone && phoneHref && <a href={`tel:${phoneHref}`}><Phone size={16} aria-hidden="true" /> {storePhone}</a>}
          {storeEmail && <a href={`mailto:${storeEmail}`}><Mail size={16} aria-hidden="true" /> {storeEmail}</a>}
          {storeAddress && <span><MapPin size={16} aria-hidden="true" /> {storeAddress}</span>}
          <span><CreditCard size={16} aria-hidden="true" /> COD · VNPAY khi khả dụng</span>
        </div>
      </div>
      <div className="site-footer-bottom"><span>© {new Date().getFullYear()} GreenFarm</span><span>Nông sản tươi · Mua sắm thuận tiện</span></div>
    </div>
  </footer>;
}
