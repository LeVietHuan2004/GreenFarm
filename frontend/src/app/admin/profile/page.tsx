import type { Metadata } from "next";
import { AdminProfile } from "@/components/admin/admin-profile";

export const metadata: Metadata = { title: "Cài đặt tài khoản quản trị" };

export default function AdminProfilePage() {
  return <AdminProfile/>;
}
