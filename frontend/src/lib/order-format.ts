export const orderStatusLabel: Record<string,string> = {
  pending:"Chờ xác nhận", processing:"Đang xử lý", ready_for_delivery:"Chờ giao hàng",
  out_for_delivery:"Đang giao", delivered:"Đã giao", completed:"Hoàn tất", canceled:"Đã hủy"
};

export function formatDateTime(value:string){ return new Intl.DateTimeFormat("vi-VN",{dateStyle:"medium",timeStyle:"short"}).format(new Date(value)); }
