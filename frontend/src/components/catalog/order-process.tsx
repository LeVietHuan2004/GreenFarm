import { ArrowUpRight, PackageCheck, Search, ShieldCheck, Truck } from "lucide-react";
import Link from "next/link";

import styles from "./order-process.module.css";

const steps = [
  {
    number: "01",
    icon: Search,
    title: "Chọn sản phẩm",
    description: "Tìm theo danh mục, mức giá hoặc tên sản phẩm. Chọn nông sản phù hợp cho bữa ăn của bạn."
  },
  {
    number: "02",
    icon: PackageCheck,
    title: "Đóng gói",
    description: "Đơn hàng được kiểm tra tồn kho, chọn lọc và đóng gói cẩn thận trước khi bàn giao vận chuyển."
  },
  {
    number: "03",
    icon: Truck,
    title: "Giao tận cửa",
    description: "Xem phí giao dự kiến khi đặt hàng, theo dõi đơn và nhận nông sản tại địa chỉ của bạn."
  }
];

export function OrderProcess() {
  return (
    <section className={styles.section} id="delivery" aria-labelledby="order-process-heading">
      <div className={styles.inner}>
        <div className={styles.intro}>
          <span className={styles.eyebrow}>Quy trình đơn hàng</span>
          <h2 id="order-process-heading">Từ nông trại đến giỏ hàng<br className={styles.desktopBreak} /> trong một hành trình rõ ràng</h2>
          <p>Mỗi đơn hàng là một hành trình được chăm chút. Chỉ ba bước đơn giản để mang nông sản tươi ngon đến căn bếp của bạn.</p>
          <Link href="/products" className={styles.link}>Bắt đầu đi chợ <ArrowUpRight size={17} aria-hidden="true" /></Link>
        </div>
        <ol className={styles.steps}>
          {steps.map(({ number, icon: Icon, title, description }) => (
            <li className={styles.card} key={number}>
              <div className={styles.cardTop}>
                <span className={styles.icon}><Icon size={20} strokeWidth={1.7} aria-hidden="true" /></span>
                <span className={styles.number} aria-hidden="true">{number}</span>
              </div>
              <h3>{title}</h3>
              <p>{description}</p>
            </li>
          ))}
        </ol>
      </div>
      <div className={styles.note}><ShieldCheck size={17} aria-hidden="true" /><span>Mua sắm an tâm, đồng hành cùng GreenFarm.</span><Link href="/contact">Liên hệ hỗ trợ <ArrowUpRight size={14} aria-hidden="true" /></Link></div>
    </section>
  );
}
