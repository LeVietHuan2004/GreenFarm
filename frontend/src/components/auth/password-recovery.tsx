"use client";

import Link from "next/link";
import { FormEvent, useEffect, useRef, useState } from "react";
import { AuthShell } from "@/components/auth/auth-shell";
import { apiClient } from "@/lib/axios-client";
import { getApiErrorMessage } from "@/lib/api-error";

export function PasswordRecovery({ mode }: { mode: "forgot" | "reset" }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [token, setToken] = useState("");
  const tokenRef = useRef("");
  const [loaded, setLoaded] = useState(mode === "forgot");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (mode !== "reset") return;
    const value = new URLSearchParams(window.location.hash.slice(1)).get("token");
    if (value) tokenRef.current = value;
    const timer = window.setTimeout(() => { if (tokenRef.current) setToken(tokenRef.current); setLoaded(true); }, 0);
    if (value) window.history.replaceState(null, "", window.location.pathname);
    return () => window.clearTimeout(timer);
  }, [mode]);

  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError(null); setMessage(null);
    if (mode === "reset" && password !== confirmation) { setError("Mật khẩu xác nhận chưa khớp."); return; }
    setBusy(true);
    try {
      if (mode === "forgot") {
        await apiClient.post("/auth/forgot-password", { email: email.trim() });
        setMessage("Nếu email thuộc tài khoản đang hoạt động, hướng dẫn đặt lại mật khẩu sẽ được gửi. Vui lòng kiểm tra cả thư rác.");
      } else {
        await apiClient.post("/auth/reset-password", { token, newPassword: password });
        setMessage("Đã đặt lại mật khẩu. Bạn có thể đăng nhập lại."); setToken(""); setPassword(""); setConfirmation("");
      }
    } catch (cause) { setError(getApiErrorMessage(cause)); }
    finally { setBusy(false); }
  };

  return <AuthShell title={mode === "forgot" ? "Quên mật khẩu" : "Đặt lại mật khẩu"} description={mode === "forgot" ? "Nhập email tài khoản để nhận hướng dẫn." : "Nhập mật khẩu mới cho tài khoản của bạn."} footer={<Link href="/login">Về trang đăng nhập</Link>}>
    {loaded && mode === "reset" && !token && !message && <p className="form-message error">Liên kết không hợp lệ. <Link href="/forgot-password">Yêu cầu liên kết mới</Link>.</p>}
    {message && <p className="form-message success" role="status">{message}</p>}
    {error && <p className="form-message error" role="alert">{error}</p>}
    {loaded && !message && (mode === "forgot" || token) && <form className="form-stack" onSubmit={submit}>
      {mode === "forgot" ? <div className="field-group"><label htmlFor="recovery-email">Email</label><input id="recovery-email" type="email" required maxLength={255} autoComplete="email" value={email} onChange={event => setEmail(event.target.value)} /></div> : <>
        <div className="field-group"><label htmlFor="new-password">Mật khẩu mới</label><input id="new-password" type="password" required minLength={8} maxLength={72} autoComplete="new-password" value={password} onChange={event => setPassword(event.target.value)} /></div>
        <div className="field-group"><label htmlFor="confirm-password">Nhập lại mật khẩu</label><input id="confirm-password" type="password" required minLength={8} maxLength={72} autoComplete="new-password" value={confirmation} onChange={event => setConfirmation(event.target.value)} /></div>
      </>}
      <button className="primary-button" disabled={busy} type="submit">{busy ? "Đang xử lý..." : mode === "forgot" ? "Gửi hướng dẫn" : "Đặt lại mật khẩu"}</button>
    </form>}
  </AuthShell>;
}
