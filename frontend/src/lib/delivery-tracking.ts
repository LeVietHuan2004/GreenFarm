export const deliveryFilters = [
  { id: "active", label: "Đang thực hiện" },
  { id: "pending", label: "Chờ xác nhận" },
  { id: "preparing", label: "Chờ giao hàng" },
  { id: "shipping", label: "Đang giao" },
  { id: "delivered", label: "Đã giao" },
  { id: "canceled", label: "Đã hủy" },
  { id: "all", label: "Tất cả" },
] as const;

export type DeliveryFilter = typeof deliveryFilters[number]["id"];

export function matchesDeliveryFilter(status: string, filter: DeliveryFilter) {
  switch (filter) {
    case "active": return !["delivered", "completed", "canceled"].includes(status);
    case "pending": return status === "pending";
    case "preparing": return ["processing", "ready_for_delivery"].includes(status);
    case "shipping": return ["out_for_delivery", "delivery_failed"].includes(status);
    case "delivered": return ["delivered", "completed"].includes(status);
    case "canceled": return status === "canceled";
    case "all": return true;
  }
}

export function deliveryStep(status: string) {
  switch (status) {
    case "pending": return 0;
    case "processing":
    case "ready_for_delivery": return 1;
    case "out_for_delivery":
    case "delivery_failed": return 2;
    case "delivered":
    case "completed": return 3;
    default: return -1;
  }
}

export function deliveryMessage(status: string) {
  switch (status) {
    case "pending": return "GreenFarm đã nhận đơn và đang chờ xác nhận.";
    case "processing": return "Đơn hàng đang được chuẩn bị và đóng gói.";
    case "ready_for_delivery": return "Đơn đã sẵn sàng, đang chờ bàn giao để vận chuyển.";
    case "out_for_delivery": return "Đơn hàng đang trên đường đến bạn. Hãy chú ý điện thoại nhé!";
    case "delivery_failed": return "Lần giao gần nhất chưa thành công. Liên hệ GreenFarm để được hỗ trợ giao lại.";
    case "delivered":
    case "completed": return "Đơn hàng đã được giao thành công. Cảm ơn bạn đã mua sắm tại GreenFarm!";
    case "canceled": return "Đơn hàng đã hủy và sẽ không tiếp tục được giao.";
    default: return "Xem chi tiết đơn hàng để biết thông tin mới nhất.";
  }
}
