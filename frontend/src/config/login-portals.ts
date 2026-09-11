import type { UserRole } from "@/types/auth";

export type LoginPortalId = "customer" | "staff" | "delivery" | "admin";

export type LoginPortal = {
  id: LoginPortalId;
  role: UserRole;
  label: string;
  href: string;
  destination: string;
  title: string;
  description: string;
  emailLabel: string;
  emailPlaceholder: string;
  buttonLabel: string;
  accessNote: string;
  visualEyebrow: string;
  visualTitle: string;
};

export const LOGIN_PORTALS: readonly LoginPortal[] = [
  {
    id: "customer",
    role: "customer",
    label: "Khách hàng",
    href: "/login",
    destination: "/",
    title: "Đăng nhập khách hàng",
    description: "Tiếp tục mua sắm và quản lý tài khoản GreenFarm.",
    emailLabel: "Email khách hàng",
    emailPlaceholder: "ban@greenfarm.vn",
    buttonLabel: "Đăng nhập cửa hàng",
    accessNote: "Cổng này chỉ dành cho tài khoản khách hàng.",
    visualEyebrow: "Cửa hàng GreenFarm",
    visualTitle: "Nông sản tươi cho bữa ăn mỗi ngày."
  },
  {
    id: "staff",
    role: "staff",
    label: "Nhan vien",
    href: "/staff/login",
    destination: "/staff",
    title: "Dang nhap nhan vien",
    description: "Truy cap khu vuc xu ly va chuan bi don hang.",
    emailLabel: "Email nhan vien",
    emailPlaceholder: "nhanvien@greenfarm.vn",
    buttonLabel: "Dang nhap van hanh",
    accessNote: "Cong nay chi danh cho tai khoan nhan vien van hanh.",
    visualEyebrow: "Khu vuc van hanh",
    visualTitle: "Xu ly don hang dung quy trinh va dung thoi diem."
  },
  {
    id: "delivery",
    role: "delivery_staff",
    label: "Giao hàng",
    href: "/delivery/login",
    destination: "/delivery",
    title: "Đăng nhập giao hàng",
    description: "Truy cập khu vực tiếp nhận và theo dõi đơn giao.",
    emailLabel: "Email nhân viên giao hàng",
    emailPlaceholder: "giaohang@greenfarm.vn",
    buttonLabel: "Đăng nhập giao hàng",
    accessNote: "Cổng này chỉ dành cho tài khoản có vai trò giao hàng.",
    visualEyebrow: "Khu vực giao hàng",
    visualTitle: "Theo dõi công việc giao nhận trong một nơi."
  },
  {
    id: "admin",
    role: "admin",
    label: "Quản trị viên",
    href: "/admin/login",
    destination: "/admin",
    title: "Đăng nhập quản trị",
    description: "Truy cập hệ thống quản lý cửa hàng GreenFarm.",
    emailLabel: "Email quản trị",
    emailPlaceholder: "admin@greenfarm.vn",
    buttonLabel: "Đăng nhập quản trị",
    accessNote: "Chỉ tài khoản có vai trò quản trị viên mới được vào hệ thống quản lý.",
    visualEyebrow: "Khu vực quản trị",
    visualTitle: "Điều phối cửa hàng từ một bảng quản lý tập trung."
  }
];

export function getPortalById(id: LoginPortalId): LoginPortal {
  return LOGIN_PORTALS.find((portal) => portal.id === id) ?? LOGIN_PORTALS[0];
}

export function getRoleHome(role?: UserRole | null): string {
  switch (role) {
    case "admin":
      return "/admin";
    case "delivery_staff":
      return "/delivery";
    case "staff":
      return "/staff";
    case "customer":
      return "/";
    default:
      return "/";
  }
}

export function getRoleLogin(role?: UserRole | null): string {
  switch (role) {
    case "admin":
      return "/admin/login";
    case "delivery_staff":
      return "/delivery/login";
    case "staff":
      return "/staff/login";
    default:
      return "/login";
  }
}
