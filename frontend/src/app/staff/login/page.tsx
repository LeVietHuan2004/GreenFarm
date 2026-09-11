import type { Metadata } from "next";
import { RoleLoginPage } from "@/components/auth/role-login-page";
import { getPortalById } from "@/config/login-portals";
export const metadata: Metadata = { title: "Dang nhap nhan vien" };
export default function StaffLoginPage() { return <RoleLoginPage portal={getPortalById("staff")} />; }
