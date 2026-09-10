import type { Metadata } from "next";
import Link from "next/link";

import { AuthShell } from "@/components/auth/auth-shell";
import { RegisterForm } from "@/components/auth/register-form";

export const metadata: Metadata = { title: "Đăng ký" };

export default function RegisterPage() {
  return (
    <AuthShell
      title="Tạo tài khoản"
      description="Thiết lập tài khoản khách hàng GreenFarm."
      footer={<><span>Đã có tài khoản?</span> <Link href="/login">Đăng nhập</Link></>}
    >
      <RegisterForm />
    </AuthShell>
  );
}
