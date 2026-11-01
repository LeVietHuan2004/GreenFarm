import { PasswordRecovery } from "@/components/auth/password-recovery";
export const metadata = { title: "Quên mật khẩu" };
export default function ForgotPasswordPage() { return <PasswordRecovery mode="forgot" />; }
