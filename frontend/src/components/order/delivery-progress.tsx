import { Check, CheckCheck, ClipboardCheck, PackageCheck, Truck } from "lucide-react";
import { deliveryStep } from "@/lib/delivery-tracking";
import styles from "./delivery-tracking.module.css";

const steps = [
  { label: "Đã đặt đơn", icon: ClipboardCheck },
  { label: "Chuẩn bị hàng", icon: PackageCheck },
  { label: "Đang giao", icon: Truck },
  { label: "Đã nhận hàng", icon: CheckCheck },
];

export function DeliveryProgress({ status }: { status: string }) {
  const current = deliveryStep(status);
  if (current < 0) return null;

  return <ol className={styles.progress} aria-label="Tiến trình giao hàng">
    {steps.map(({ label, icon: Icon }, index) => <li key={label}
      className={`${index <= current ? styles.reached : ""} ${index === current ? styles.current : ""}`}
      aria-current={index === current ? "step" : undefined}>
      <span>{index < current ? <Check size={18} aria-hidden="true" /> : <Icon size={18} aria-hidden="true" />}</span>
      <small>{label}</small>
      <span className={styles.srOnly}>{index < current ? "Đã hoàn thành" : index === current ? "Hiện tại" : "Chưa tới bước này"}</span>
    </li>)}
  </ol>;
}
