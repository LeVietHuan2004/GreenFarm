import { PackageCheck, Truck } from "lucide-react";
import { formatDateTime } from "@/lib/order-format";
import type { Shipment } from "@/types/order";

const statusLabels: Record<string, string> = {
  not_created: "Shop đang chuẩn bị hàng",
  created: "Đã tạo vận đơn",
  ready_to_pick: "Chờ GHN lấy hàng",
  picking: "GHN đang lấy hàng",
  picked: "GHN đã lấy hàng",
  storing: "Đang ở kho GHN",
  transporting: "Đang vận chuyển",
  sorting: "Đang phân loại",
  delivering: "Đang giao hàng",
  delivered: "Đã giao hàng",
  delivery_fail: "Giao hàng chưa thành công",
  return: "Đang hoàn hàng",
  returned: "Đã hoàn hàng",
  cancel: "Đã hủy vận đơn",
};

export function GhnShipmentTracking({ shipment }: { shipment: Shipment | null }) {
  return <section className="order-totals ghn-tracking">
    <h2><Truck size={17}/>Giao hàng nhanh (GHN)</h2>
    <div><span>Trạng thái</span><strong>{statusLabels[shipment?.shippingStatus ?? "not_created"] ?? shipment?.shippingStatus}</strong></div>
    {shipment?.providerOrderCode && <div><span>Mã vận đơn</span><strong>{shipment.providerOrderCode}</strong></div>}
    {shipment?.estimatedDeliveryAt && <div><span>Dự kiến giao</span><strong>{formatDateTime(shipment.estimatedDeliveryAt)}</strong></div>}
    {!shipment?.providerOrderCode && <p><PackageCheck size={15}/>Shop sẽ tạo vận đơn sau khi chuẩn bị hàng xong.</p>}
  </section>;
}
