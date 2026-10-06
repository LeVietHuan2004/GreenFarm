"use client";

import { Eye, EyeOff, LogIn } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { getApiErrorMessage } from "@/lib/api-error";
import type { LoginPortal } from "@/config/login-portals";
import { authService } from "@/services/auth-service";
import { guestCommerceService } from "@/services/guest-commerce-service";
import { clearGuestSession, getGuestSession } from "@/lib/guest-session";
import { useAuthStore } from "@/stores/auth-store";

type LoginValues = {
  email: string;
  password: string;
};

export function LoginForm({ portal }: { portal: LoginPortal }) {
  const router = useRouter();
  const setSession = useAuthStore((state) => state.setSession);
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<LoginValues>();

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null);
    try {
      const session = await authService.login({
        email: values.email.trim(),
        password: values.password,
        role: portal.role
      });

      if (session.user.role !== portal.role) {
        setServerError("Tài khoản không thuộc cổng đăng nhập đã chọn.");
        return;
      }
      setSession(session);
      if (portal.role === "customer" && getGuestSession()) {
        try { await guestCommerceService.merge(); clearGuestSession(); }
        catch (error) { setServerError(`Đăng nhập thành công nhưng chưa thể hợp nhất giỏ hàng: ${getApiErrorMessage(error)}`); return; }
      }
      router.replace(portal.destination);
    } catch (error) {
      setServerError(getApiErrorMessage(error));
    }
  });

  return (
    <form onSubmit={onSubmit} className="form-stack" noValidate>
      <div className="field-group">
        <label htmlFor="email">{portal.emailLabel}</label>
        <input
          id="email"
          type="email"
          autoComplete="email"
          placeholder={portal.emailPlaceholder}
          aria-invalid={Boolean(errors.email)}
          {...register("email", {
            required: "Vui lòng nhập email.",
            pattern: {
              value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
              message: "Email chưa đúng định dạng."
            }
          })}
        />
        {errors.email && <p className="field-error">{errors.email.message}</p>}
      </div>

      <div className="field-group">
        <div className="field-label-row">
          <label htmlFor="password">Mật khẩu</label>
          <span>Tối thiểu 8 ký tự</span>
        </div>
        <div className="password-field">
          <input
            id="password"
            type={showPassword ? "text" : "password"}
            autoComplete="current-password"
            placeholder="Nhập mật khẩu"
            aria-invalid={Boolean(errors.password)}
            {...register("password", {
              required: "Vui lòng nhập mật khẩu."
            })}
          />
          <button
            type="button"
            className="password-toggle"
            onClick={() => setShowPassword((value) => !value)}
            aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            title={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
        {errors.password && <p className="field-error">{errors.password.message}</p>}
      </div>

      {serverError && <p className="form-message error" role="alert">{serverError}</p>}

      <button type="submit" className="primary-button" disabled={isSubmitting}>
        <LogIn size={18} aria-hidden="true" />
        {isSubmitting ? "Đang đăng nhập..." : portal.buttonLabel}
      </button>

      <p className="portal-access-note">
        {portal.accessNote}
      </p>
    </form>
  );
}
